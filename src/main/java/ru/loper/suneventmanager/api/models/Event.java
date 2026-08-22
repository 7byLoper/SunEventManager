package ru.loper.suneventmanager.api.models;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;
import ru.loper.suneventmanager.SunEventManager;
import ru.loper.suneventmanager.api.enums.EventState;
import ru.loper.suneventmanager.bossbar.EventBossBarController;
import ru.loper.suneventmanager.config.EventConfigManager;
import ru.loper.suneventmanager.finder.LocationFinder;
import ru.loper.suneventmanager.hologram.EventHologramController;
import ru.loper.suneventmanager.manager.EventManager;
import ru.loper.suneventmanager.manager.SchematicManager;
import ru.loper.suneventmanager.utils.TimeUtils;

@Getter
public abstract class Event {
    protected final String id;
    protected final UUID uniqueId;

    protected final EventConfigManager configManager;
    protected final LocationFinder locationFinder;
    protected final EventManager eventManager;
    private final EventBossBarController bossBarController;
    private final EventHologramController eventHologramController;
    protected SchematicManager schematicManager;

    @Setter
    protected EventRarity eventRarity;

    protected EventState currentState;
    protected Location eventLocation;

    @Setter
    protected String summonedBy;

    protected boolean findLocation;
    protected int eventTimer;
    protected boolean activate;
    private boolean stopping;

    public Event(String id, EventConfigManager configManager) {
        this.id = id;
        this.uniqueId = UUID.randomUUID();
        this.configManager = configManager;
        this.locationFinder = new LocationFinder(this, configManager.getGeneratorSettings());
        this.eventManager = SunEventManager.getInstance().getEventManager();
        this.bossBarController = new EventBossBarController(this, configManager.getBossBarSettings());
        this.eventHologramController = new EventHologramController(this);
    }

    public void setRandomRarity() {
        this.eventRarity = configManager.getRarityManager().getRandomRarity().orElse(null);

        if (eventRarity == null) {
            SunEventManager.getInstance()
                    .getLogger()
                    .severe("Event '" + id + "' cannot start: no valid rarities are configured.");
        }
    }

    public void eventTimerProgress() {
        if (!activate || currentState == null) {
            return;
        }

        if (eventTimer > 0) {
            eventTimer--;
        }

        if (eventTimer <= 0) {
            updateState(currentState);
        }

        if (activate
                && (currentState == EventState.STARTED
                        || currentState == EventState.OPENED
                        || currentState == EventState.STOPPING)) {
            eventRarity.getChestsManager().updateHolograms(this);
            eventHologramController.update();
            bossBarController.update();
        }
    }

    public String getTimerMessage() {
        if (currentState == null || !configManager.isInfoMessagesEnabled()) {
            return "";
        }

        return switch (currentState) {
            case PRESTART -> configManager.getInfoMessagePrestart(this);
            case STARTED -> configManager.getInfoMessageStart(this);
            case OPENED -> configManager.getInfoMessageOpen(this);
            case STOPPING ->
                configManager.isOpenedStageEnabled()
                        ? configManager.getInfoMessageStop(this)
                        : configManager.getInfoMessageOpen(this);
            default -> "";
        };
    }

    public String getTimerMessage(int index) {
        return getTimerMessage();
    }

    public Map<String, String> getMessagePlaceholders() {
        Map<String, String> placeholders = new LinkedHashMap<>();
        String eventName = configManager.getDisplayName();
        if (eventName == null || eventName.isBlank()) {
            eventName = id;
        }

        String rarityName = eventRarity == null ? "" : eventRarity.getDisplayName();
        int secondsLeft = Math.max(0, eventTimer);
        String timer = String.valueOf(secondsLeft);
        String timerPretty = TimeUtils.formatSeconds(secondsLeft);

        placeholders.put("event", id);
        placeholders.put("id", id);
        placeholders.put("name", eventName);
        placeholders.put("rarity", rarityName);
        placeholders.put("inventory", rarityName);
        placeholders.put("timer", timer);
        placeholders.put("timer_pretty", timerPretty);
        placeholders.put("time", timer);
        placeholders.put("TimeOpen", timer);
        placeholders.put("TimeEvent", timer);
        placeholders.put("player", summonedBy == null ? "" : summonedBy);

        if (eventLocation == null) {
            placeholders.put("x", "-");
            placeholders.put("y", "-");
            placeholders.put("z", "-");
            placeholders.put("world", "-");
            placeholders.put("cords", "-");
            return placeholders;
        }

        String x = String.valueOf(eventLocation.getBlockX());
        String y = String.valueOf(eventLocation.getBlockY());
        String z = String.valueOf(eventLocation.getBlockZ());
        String world = eventLocation.getWorld() == null
                ? "-"
                : eventLocation.getWorld().getName();

        placeholders.put("x", x);
        placeholders.put("y", y);
        placeholders.put("z", z);
        placeholders.put("world", world);
        placeholders.put("cords", x + " " + y + " " + z);
        return placeholders;
    }

    public boolean isStarted() {
        return currentState == EventState.PRESTART
                || currentState == EventState.STARTED
                || currentState == EventState.OPENED
                || currentState == EventState.STOPPING;
    }

    public void activate() {
        if (activate) {
            return;
        }

        if (eventRarity == null) {
            setRandomRarity();
        }
        if (eventRarity == null) {
            return;
        }

        String schematicFileName = eventRarity.getSchematicFileName();
        if (schematicFileName != null && !schematicFileName.isBlank()) {
            var clipboard = eventRarity.getClipboard();
            if (clipboard == null) {
                SunEventManager.getInstance()
                        .getLogger()
                        .severe("Event '" + id + "' cannot start: schematic '" + schematicFileName
                                + "' is unavailable.");
                return;
            }
            schematicManager = new SchematicManager(clipboard);
        } else {
            schematicManager = null;
        }
        currentState = EventState.FIND_LOCATION;
        eventTimer = 0;
        activate = true;
        eventManager.registerActiveEvent(this);
    }

    public void updateState(EventState eventState) {
        if (!activate || eventState == null) {
            return;
        }

        switch (eventState) {
            case FIND_LOCATION -> handleFindLocation();
            case PRESTART -> updatePrestart();
            case STARTED -> updateStarted();
            case OPENED -> updateOpened();
            case STOPPING -> updateStopping();
        }
    }

    protected void handleFindLocation() {
        if (!findLocation) {
            findLocation = true;
            if (eventLocation == null) {
                locationFinder.findLocation();
                eventTimer = 1;
                return;
            }
        }

        if (eventLocation == null) {
            eventTimer = 1;
            return;
        }

        eventRarity.broadcastPrestart(this);
        eventTimer = configManager.getEventPrestartTime();
        currentState = EventState.PRESTART;
        scanSchematic();
        prestart();
    }

    protected void scanSchematic() {
        if (schematicManager == null) {
            return;
        }

        Material chestMarker = eventRarity.getChestsManager().getSchematicMaterial();
        if (chestMarker != null) {
            schematicManager.setSearchMaterials(chestMarker);
        }

        schematicManager.startScanClipboardBlocks(
                eventLocation,
                eventRarity.getSchemeOffset(),
                SunEventManager.getInstance().getConfigManager().getSchematicScanBlocksPerTick());
    }

    public void updatePrestart() {
        if (schematicManager != null && schematicManager.isScanning()) {
            eventTimer = 1;
            return;
        }

        if (schematicManager != null && schematicManager.isScanFailed()) {
            cancelLocationSearch("ошибка сканирования схематики");
            return;
        }

        if (schematicManager != null) {
            schematicManager.pasteClipboard(
                    eventLocation, eventRarity.getSchemeOffset(), eventRarity.isIgnoreAirBlocks());
            if (!schematicManager.isPasted()) {
                cancelLocationSearch("ошибка вставки схематики");
                return;
            }
        }

        eventRarity.broadcastStart(this);
        eventTimer = configManager.getEventStartTime();
        eventRarity.createRegion(this);
        eventRarity.getChestsManager().loadChests(this);
        currentState = EventState.STARTED;
        bossBarController.update();
        start();
        eventHologramController.update();
    }

    public void updateStarted() {
        if (!activate) {
            return;
        }

        eventRarity.getChestsManager().openChests(this);
        eventRarity.broadcastOpen(this);
        open();

        if (configManager.isOpenedStageEnabled()) {
            eventTimer = configManager.getEventOpenedTime();
            currentState = EventState.OPENED;
            bossBarController.update();
            if (eventTimer <= 0) {
                updateOpened();
            }
            return;
        }

        // Legacy behavior: STOPPING is the open/active stage.
        eventTimer = configManager.getEventStopTime();
        currentState = EventState.STOPPING;
        bossBarController.update();
    }

    public void updateOpened() {
        if (!activate) {
            return;
        }

        eventRarity.broadcastEnding(this);
        ending();
        if (configManager.isCloseChestsOnStopping()) {
            eventRarity.getChestsManager().closeChests(this);
        }
        eventTimer = configManager.getEventStopTime();
        currentState = EventState.STOPPING;
        bossBarController.update();

        if (eventTimer <= 0) {
            updateStopping();
        }
    }

    public void updateStopping() {
        if (!activate || stopping) {
            return;
        }

        stopping = true;
        eventTimer = 0;

        try {
            cleanup("hide bossbar", bossBarController::hide);
            cleanup("remove event hologram", eventHologramController::remove);
            cleanup("broadcast stop", () -> eventRarity.broadcastStop(this));
            cleanup("remove chests", () -> eventRarity.getChestsManager().removeChests(this));
            cleanup("remove region", () -> eventRarity.removeRegion(this));
            if (schematicManager != null) {
                cleanup("remove schematic", schematicManager::removeSchematic);
            }
            cleanup("event remove hook", this::remove);
        } finally {
            activate = false;
            stopping = false;
            eventManager.unregisterActiveEvent(this);
        }
    }

    private void cleanup(String stage, Runnable action) {
        try {
            action.run();
        } catch (Exception exception) {
            SunEventManager.getInstance()
                    .getLogger()
                    .log(
                            java.util.logging.Level.SEVERE,
                            "Ошибка очистки ивента '" + id + "' на этапе " + stage,
                            exception);
        }
    }

    public Location getEventHologramAnchor() {
        return eventLocation == null ? null : eventLocation.clone();
    }

    public void setEventLocation(Location eventLocation) {
        this.eventLocation = eventLocation == null ? null : eventLocation.clone();
        findLocation = eventLocation != null;
    }

    public boolean setEventTimer(int eventTimer) {
        if (eventTimer < 0) {
            return false;
        }

        this.eventTimer = eventTimer;
        return true;
    }

    public void cancelLocationSearch(String reason) {
        if (!activate) {
            return;
        }

        SunEventManager.getInstance()
                .getMessageService()
                .broadcast(
                        SunEventManager.getInstance().getConfigManager().getErrorFindLocationMessage(),
                        getMessagePlaceholders());
        SunEventManager.getInstance().getLogger().warning("Event '" + id + "' cancelled: " + reason);

        bossBarController.hide();
        eventHologramController.remove();
        activate = false;
        eventTimer = 0;
        if (schematicManager != null) {
            schematicManager.removeSchematic();
        }
        eventManager.unregisterActiveEvent(this);
    }

    public abstract void prestart();

    public abstract void start();

    public abstract void open();

    /**
     * Optional transition between the active/open stage and final removal.
     * Only used when the event config explicitly defines timers.opened/active.
     */
    public void ending() {}

    public abstract void remove();
}
