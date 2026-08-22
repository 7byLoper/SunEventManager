package ru.loper.suneventmanager.summoner.block.service;

import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.entity.Player;
import ru.loper.suneventmanager.SunEventManager;
import ru.loper.suneventmanager.manager.EventManager;
import ru.loper.suneventmanager.manager.SummonBlockManager;
import ru.loper.suneventmanager.summoner.block.model.SummonBlockDefinition;
import ru.loper.suneventmanager.summoner.block.model.SummonMessages;

public final class SummonMessageService {
    private final SummonBlockManager manager;

    public SummonMessageService(SummonBlockManager manager) {
        this.manager = manager;
    }

    public void cooldown(Player player, SummonBlockDefinition definition, long seconds) {
        send(player, messages(definition).cooldown(), definition, null, seconds, null);
    }

    public void success(Player player, SummonBlockDefinition definition, EventManager.EventStartResult result) {
        send(player, messages(definition).success(), definition, result, 0L, null);
    }

    public void validationFailure(
            Player player, SummonBlockDefinition definition, SummonValidator.ValidationResult result) {
        SummonMessages messages = messages(definition);
        String message =
                switch (result) {
                    case MIN_Y -> messages.minY();
                    case MAX_Y -> messages.maxY();
                    case BLOCKED_BLOCK -> messages.blockedBlock();
                    case WORLD -> messages.world();
                    case SURFACE -> messages.surface();
                    case NEAR_EVENT -> messages.nearEvent();
                    case SKY -> messages.sky();
                    case REGION -> messages.region();
                    case INVALID_LOCATION -> messages.startError();
                    case OK -> "";
                };
        send(player, message, definition, null, 0L, result.name());
    }

    public void startFailure(Player player, SummonBlockDefinition definition, EventManager.EventStartResult result) {
        SummonMessages messages = messages(definition);
        String message =
                switch (result.error()) {
                    case UNKNOWN_EVENT -> messages.unknownEvent();
                    case UNKNOWN_RARITY -> messages.unknownRarity();
                    default -> messages.startError();
                };
        send(player, message, definition, result, 0L, result.error().name());
    }

    private void send(
            Player player,
            String message,
            SummonBlockDefinition definition,
            EventManager.EventStartResult result,
            long seconds,
            String error) {
        if (player == null || message == null || message.isEmpty()) {
            return;
        }

        String eventId = result == null ? eventId(definition.eventSpec()) : result.eventId();
        String rarity = result == null ? rarity(definition.eventSpec()) : result.rarityName();
        if (result != null
                && result.success()
                && result.event() != null
                && result.event().getEventRarity() != null) {
            rarity = result.event().getEventRarity().getDisplayName();
        }

        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("id", definition.id());
        placeholders.put("event", eventId);
        placeholders.put("rarity", rarity == null || rarity.isBlank() ? "random" : rarity);
        placeholders.put("inventory", rarity == null || rarity.isBlank() ? "random" : rarity);
        placeholders.put("time", String.valueOf(seconds));
        placeholders.put("minY", String.valueOf(definition.checks().minY()));
        placeholders.put("maxY", String.valueOf(definition.checks().maxY()));
        placeholders.put("min_y", String.valueOf(definition.checks().minY()));
        placeholders.put("max_y", String.valueOf(definition.checks().maxY()));
        placeholders.put("world", player.getWorld().getName());
        placeholders.put("error", error == null ? "" : error);
        placeholders.put("player", player.getName());

        SunEventManager.getInstance().getMessageService().send(player, message, placeholders);
    }

    private SummonMessages messages(SummonBlockDefinition definition) {
        if (definition != null && definition.messages() != null) {
            return definition.messages();
        }
        return manager.getMessages();
    }

    private String eventId(String eventSpec) {
        if (eventSpec == null || eventSpec.isBlank()) {
            return "";
        }
        int separator = eventSpec.indexOf(':');
        return separator < 0 ? eventSpec : eventSpec.substring(0, separator);
    }

    private String rarity(String eventSpec) {
        if (eventSpec == null) {
            return null;
        }
        int separator = eventSpec.indexOf(':');
        if (separator < 0 || separator + 1 >= eventSpec.length()) {
            return null;
        }
        return eventSpec.substring(separator + 1);
    }
}
