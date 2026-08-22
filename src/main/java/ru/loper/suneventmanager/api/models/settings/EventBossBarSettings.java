package ru.loper.suneventmanager.api.models.settings;

import java.util.Locale;
import lombok.Builder;
import lombok.Value;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.ConfigurationSection;

@Value
@Builder
public class EventBossBarSettings {
    boolean enabled;
    String startedTitle;
    String openedTitle;
    String stoppingTitle;
    BarColor color;
    BarStyle style;
    double radius;

    public static EventBossBarSettings fromSection(ConfigurationSection section) {
        if (section == null || !section.getBoolean("enabled", section.getBoolean("toggle", false))) {
            return EventBossBarSettings.builder().enabled(false).build();
        }

        return EventBossBarSettings.builder()
                .enabled(true)
                .startedTitle(first(section, "titles.started", "started", ""))
                .openedTitle(first(section, "titles.opened", "titles.open", "activated", ""))
                .stoppingTitle(first(section, "titles.stopping", "titles.stop", "destroy", ""))
                .color(parseColor(section.getString("color", "RED")))
                .style(parseStyle(section.getString("style", "SEGMENTED_20")))
                .radius(section.getDouble("radius", -1.0D))
                .build();
    }

    private static String first(ConfigurationSection section, String first, String second, String fallback) {
        String value = section.getString(first);
        return value == null ? section.getString(second, fallback) : value;
    }

    private static String first(
            ConfigurationSection section, String first, String second, String third, String fallback) {
        String value = section.getString(first);
        if (value != null) {
            return value;
        }
        value = section.getString(second);
        return value == null ? section.getString(third, fallback) : value;
    }

    private static BarColor parseColor(String value) {
        try {
            return BarColor.valueOf(value == null ? "RED" : value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return BarColor.RED;
        }
    }

    private static BarStyle parseStyle(String value) {
        try {
            return BarStyle.valueOf(
                    value == null ? "SEGMENTED_20" : value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return BarStyle.SEGMENTED_20;
        }
    }
}
