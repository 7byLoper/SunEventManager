package ru.loper.suneventmanager.api.message;

import java.util.Collections;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;
import ru.loper.suneventmanager.SunEventManager;

/**
 * Public message facade for event plugins.
 * Keeps MiniMessage, local placeholders and PlaceholderAPI parsing centralized in SunEventManager.
 */
public final class EventMessages {
    private EventMessages() {}

    public static void send(
            @Nullable CommandSender viewer, @Nullable String message, @Nullable Map<String, ?> placeholders) {
        SunEventManager plugin = SunEventManager.getInstance();
        if (plugin == null || plugin.getMessageService() == null) {
            return;
        }

        plugin.getMessageService().send(viewer, message, safe(placeholders));
    }

    public static String legacy(
            @Nullable CommandSender viewer, @Nullable String message, @Nullable Map<String, ?> placeholders) {
        SunEventManager plugin = SunEventManager.getInstance();
        if (plugin == null || plugin.getMessageService() == null) {
            return message == null ? "" : message;
        }

        return plugin.getMessageService().toLegacy(viewer, message, safe(placeholders));
    }

    private static Map<String, ?> safe(@Nullable Map<String, ?> placeholders) {
        return placeholders == null ? Collections.emptyMap() : placeholders;
    }
}
