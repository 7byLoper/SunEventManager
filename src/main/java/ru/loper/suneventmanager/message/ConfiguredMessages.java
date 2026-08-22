package ru.loper.suneventmanager.message;

import java.util.Collections;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.loper.suneventmanager.SunEventManager;

public final class ConfiguredMessages {
    private ConfiguredMessages() {}

    public static void send(
            @Nullable CommandSender viewer, @NotNull String path, @NotNull String fallback, Object... placeholders) {
        SunEventManager plugin = SunEventManager.getInstance();
        if (plugin == null || plugin.getMessageService() == null) {
            return;
        }

        Map<String, String> values = placeholders == null || placeholders.length == 0
                ? Collections.emptyMap()
                : plugin.getMessageService().placeholders(placeholders);
        plugin.getMessageService().sendConfigured(viewer, path, fallback, values);
    }
}
