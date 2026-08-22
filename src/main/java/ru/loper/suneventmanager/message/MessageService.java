package ru.loper.suneventmanager.message;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.loper.suncore.api.colorize.ComponentColorize;
import ru.loper.suncore.api.colorize.TextFormatter;
import ru.loper.suneventmanager.SunEventManager;

/**
 * Единый фасад сообщений EventManager.
 *
 * <p>Локальные плейсхолдеры события подставляются здесь, а PlaceholderAPI,
 * theme placeholders, legacy colors и MiniMessage всегда обрабатываются
 * общим {@link TextFormatter} из SunCore. Так сообщения, bossbar, hologram
 * и команды используют один и тот же порядок рендера.</p>
 */
public final class MessageService {
    private final SunEventManager plugin;

    public MessageService(@NotNull SunEventManager plugin) {
        this.plugin = plugin;
    }

    public void send(@Nullable CommandSender viewer, @Nullable String raw, @Nullable Map<String, ?> placeholders) {
        if (viewer == null || raw == null || raw.isEmpty()) {
            return;
        }

        OfflinePlayer context = viewer instanceof Player player ? player : null;
        try {
            viewer.sendMessage(render(viewer, raw, placeholders));
        } catch (NoSuchMethodError | AbstractMethodError error) {
            viewer.sendMessage(TextFormatter.legacy(context, prepare(raw, placeholders)));
        }
    }

    public void sendConfigured(
            @Nullable CommandSender viewer,
            @NotNull String path,
            @NotNull String fallback,
            @Nullable Map<String, ?> placeholders) {
        String message = fallback;
        if (plugin.getConfigManager() != null) {
            message = plugin.getConfigManager().getMessage(path, fallback);
        }
        send(viewer, message, placeholders);
    }

    public void sendConfiguredLines(
            @Nullable CommandSender viewer,
            @NotNull String path,
            @NotNull List<String> fallback,
            @Nullable Map<String, ?> placeholders) {
        List<String> lines = fallback;
        if (plugin.getConfigManager() != null) {
            lines = plugin.getConfigManager().getMessageList(path, fallback);
        }
        sendLines(viewer, lines, placeholders);
    }

    public void sendLines(
            @Nullable CommandSender viewer, @Nullable List<String> lines, @Nullable Map<String, ?> placeholders) {
        if (viewer == null || lines == null || lines.isEmpty()) {
            return;
        }

        for (String line : lines) {
            send(viewer, line, placeholders);
        }
    }

    public void broadcast(@Nullable List<String> lines, @Nullable Map<String, ?> placeholders) {
        if (lines == null || lines.isEmpty()) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            sendLines(player, lines, placeholders);
        }
    }

    public void broadcast(@Nullable String raw, @Nullable Map<String, ?> placeholders) {
        if (raw == null || raw.isEmpty()) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            send(player, raw, placeholders);
        }
    }

    public Component render(
            @Nullable CommandSender viewer, @Nullable String raw, @Nullable Map<String, ?> placeholders) {
        OfflinePlayer context = viewer instanceof Player player ? player : null;
        return TextFormatter.component(context, prepare(raw, placeholders));
    }

    public String toLegacy(
            @Nullable CommandSender viewer, @Nullable String raw, @Nullable Map<String, ?> placeholders) {
        return ComponentColorize.convertLegacyString(render(viewer, raw, placeholders));
    }

    public List<String> toLegacyLines(@Nullable List<String> lines, @Nullable Map<String, ?> placeholders) {
        if (lines == null || lines.isEmpty()) {
            return Collections.emptyList();
        }

        return lines.stream().map(line -> toLegacy(null, line, placeholders)).toList();
    }

    public Map<String, String> placeholders(Object... keyValues) {
        Map<String, String> values = new LinkedHashMap<>();
        if (keyValues == null) {
            return values;
        }

        for (int index = 0; index + 1 < keyValues.length; index += 2) {
            Object key = keyValues[index];
            if (key == null) {
                continue;
            }

            Object value = keyValues[index + 1];
            values.put(String.valueOf(key), value == null ? "" : String.valueOf(value));
        }
        return values;
    }

    private String prepare(@Nullable String raw, @Nullable Map<String, ?> placeholders) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        return LegacyMiniMessageAdapter.convert(
                TextFormatter.replace(raw, placeholders == null ? Collections.emptyMap() : placeholders));
    }
}
