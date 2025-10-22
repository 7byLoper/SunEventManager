package ru.loper.suneventmanager.api.modules.event;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import ru.loper.suneventmanager.SunEventManager;
import ru.loper.suneventmanager.api.modules.event.rarity.EventRarity;
import ru.loper.suneventmanager.config.EventConfigManager;
import ru.loper.suneventmanager.hook.RegionManager;
import ru.loper.suneventmanager.hook.SchematicManager;
import ru.loper.suneventmanager.manager.LocationFinder;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Getter
public abstract class Event {
    protected final EventConfigManager configManager;
    protected final LocationFinder locationFinder;
    protected final EventManager eventManager;
    protected final UUID uuid;
    protected final String id;
    protected SchematicManager schematicManager;
    @Setter
    protected EventRarity eventRarity;
    protected EventState currentState;
    protected Location eventLocation;
    protected boolean findLocation;
    protected int eventTimer;
    protected boolean activate;

    public Event(String id, EventConfigManager configManager) {
        this.configManager = configManager;
        this.eventManager = SunEventManager.getInstance().getEventManager();
        this.locationFinder = new LocationFinder(this);
        this.uuid = UUID.randomUUID();
        this.id = id;
        activate = false;
        this.eventRarity = configManager.getRarityManager().getRandomRarity().orElseThrow();
        this.findLocation = false;
        this.eventTimer = 0;
    }

    public void eventTimerProgress() {
        eventTimer--;
        if (eventTimer <= 0) {
            updateState(currentState);
        }

        if (currentState.equals(EventState.STARTED) || currentState.equals(EventState.STOPPING)) {
            eventRarity.getChestsManager().updateHolograms(this);
        }
    }

    public String getTimerMessage() {
        return switch (currentState) {
            case PRESTART -> configManager.getInfoMessagePrestart(this);
            case STARTED -> configManager.getInfoMessageStart(this);
            case STOPPING -> configManager.getInfoMessageStop(this);
            default -> "";
        };
    }

    public boolean isStarted() {
        return currentState.equals(EventState.STARTED) || currentState.equals(EventState.STOPPING) || currentState.equals(EventState.PRESTART);
    }

    public void activate() {
        schematicManager = new SchematicManager(eventRarity.getClipboard());
        currentState = EventState.FIND_LOCATION;
        eventManager.registerActiveEvent(this);
        activate = true;
    }

    public void updateState(EventState eventState) {
        switch (eventState) {
            case FIND_LOCATION -> handleFindLocation();
            case PRESTART -> updatePrestart();
            case STARTED -> updateStarted();
            case STOPPING -> updateStopping();
        }
    }

    protected void handleFindLocation() {
        if (!findLocation) {
            locationFinder.findLocation();
            findLocation = true;
            eventTimer = 1;
            return;
        }

        if (eventLocation == null) {
            eventTimer = 1;
            return;
        }

        eventRarity.broadcastPrestart();
        eventTimer = configManager.getEventPrestartTime();
        CompletableFuture.runAsync(this::scanSchematic);
        currentState = EventState.PRESTART;
        prestart();
    }

    protected void scanSchematic() {
        schematicManager.scanClipboardBlocks(eventLocation, eventRarity.getSchemeOffset());
    }

    public void updatePrestart() {
        eventRarity.broadcastStart(
                eventLocation.getBlockX(),
                eventLocation.getBlockY(),
                eventLocation.getBlockZ()
        );
        eventTimer = configManager.getEventStartTime();
        if (configManager.isRegion()) RegionManager.create(this);
        schematicManager.pasteClipboard(eventLocation, eventRarity.getSchemeOffset());
        eventRarity.getChestsManager().loadChests(this);
        currentState = EventState.STARTED;
        start();
    }

    public void updateStarted() {
        eventRarity.getChestsManager().openChests(this);
        eventTimer = configManager.getEventStopTime();
        currentState = EventState.STOPPING;
        open();
    }

    public void updateStopping() {
        eventRarity.broadcastStop();
        eventRarity.getChestsManager().removeChests(this);
        RegionManager.remove(this);
        schematicManager.removeSchematic();
        eventManager.unregisterActiveEvent(this);
        remove();
    }

    public void setEventLocation(Location eventLocation) {
        this.eventLocation = eventLocation;
        findLocation = eventLocation != null;
    }

    public abstract void prestart();

    public abstract void start();

    public abstract void open();

    public abstract void remove();
}