package ru.loper.suneventmanager.hologram;

import java.util.Collections;
import java.util.List;
import org.bukkit.Location;
import ru.loper.suncore.api.hook.holograms.HologramsServices;
import ru.loper.suneventmanager.SunEventManager;
import ru.loper.suneventmanager.api.enums.EventState;
import ru.loper.suneventmanager.api.models.Event;
import ru.loper.suneventmanager.api.models.EventRarity;

public final class EventHologramController {
    private final Event event;
    private final String hologramName;
    private boolean created;

    public EventHologramController(Event event) {
        this.event = event;
        this.hologramName = "event_%s_%s".formatted(event.getId(), event.getUniqueId());
    }

    public void update() {
        EventRarity rarity = event.getEventRarity();
        if (rarity == null || !rarity.isEventHologram()) {
            remove();
            return;
        }

        Location anchor = event.getEventHologramAnchor();
        if (anchor == null || anchor.getWorld() == null) {
            return;
        }

        List<String> lines = lines(rarity);
        if (lines.isEmpty()) {
            return;
        }

        List<String> rendered =
                SunEventManager.getInstance().getMessageService().toLegacyLines(lines, event.getMessagePlaceholders());
        Location location = anchor.clone().add(rarity.getHologramOffset());

        try {
            HologramsServices.decentHolograms().createHologram(hologramName, location, rendered);
            created = true;
        } catch (RuntimeException exception) {
            SunEventManager.getInstance()
                    .getLogger()
                    .warning(
                            "Не удалось обновить голограмму ивента '" + event.getId() + "': " + exception.getMessage());
        }
    }

    public void remove() {
        if (!created) {
            return;
        }

        try {
            HologramsServices.decentHolograms().removeHologram(hologramName);
        } catch (RuntimeException exception) {
            SunEventManager.getInstance()
                    .getLogger()
                    .warning("Не удалось удалить голограмму ивента '" + event.getId() + "': " + exception.getMessage());
        } finally {
            created = false;
        }
    }

    private List<String> lines(EventRarity rarity) {
        EventState state = event.getCurrentState();
        if (state == null) {
            return Collections.emptyList();
        }

        return switch (state) {
            case STARTED -> rarity.getHologramStart();
            case OPENED -> rarity.getHologramOpen();
            case STOPPING ->
                event.getConfigManager().isOpenedStageEnabled()
                        ? rarity.getHologramStop()
                        : rarity.getHologramOpen().isEmpty() ? rarity.getHologramStop() : rarity.getHologramOpen();
            default -> Collections.emptyList();
        };
    }
}
