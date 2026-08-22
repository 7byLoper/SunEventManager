package ru.loper.suneventmanager.api.models.settings;

import java.util.Collections;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;

public record EventMessageSettings(boolean enabled, List<String> lines) {
    public EventMessageSettings {
        lines = lines == null ? Collections.emptyList() : List.copyOf(lines);
    }

    public static EventMessageSettings load(ConfigurationSection parent, String... aliases) {
        if (parent == null || aliases == null) {
            return disabled();
        }

        for (String alias : aliases) {
            if (alias == null || alias.isBlank()) {
                continue;
            }

            if (parent.isList(alias)) {
                return new EventMessageSettings(true, parent.getStringList(alias));
            }

            ConfigurationSection section = parent.getConfigurationSection(alias);
            if (section != null) {
                return new EventMessageSettings(
                        section.getBoolean("enabled", section.getBoolean("toggle", true)),
                        section.getStringList("lines"));
            }
        }

        return disabled();
    }

    private static EventMessageSettings disabled() {
        return new EventMessageSettings(false, Collections.emptyList());
    }
}
