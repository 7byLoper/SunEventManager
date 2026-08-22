package ru.loper.suneventmanager.summoner.block.config;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.Vector;
import ru.loper.suneventmanager.utils.LocationUtils;

final class SummonConfigValueReader {

    String string(ConfigurationSection section, ConfigurationSection fallback, String path, String defaultValue) {
        if (section != null && section.contains(path)) {
            return section.getString(path, defaultValue);
        }
        if (fallback != null && fallback.contains(path)) {
            return fallback.getString(path, defaultValue);
        }
        return defaultValue;
    }

    int integer(ConfigurationSection section, ConfigurationSection fallback, String path, int defaultValue) {
        if (section != null && section.contains(path)) {
            return section.getInt(path, defaultValue);
        }
        if (fallback != null && fallback.contains(path)) {
            return fallback.getInt(path, defaultValue);
        }
        return defaultValue;
    }

    long longValue(ConfigurationSection section, ConfigurationSection fallback, String path, long defaultValue) {
        if (section != null && section.contains(path)) {
            return section.getLong(path, defaultValue);
        }
        if (fallback != null && fallback.contains(path)) {
            return fallback.getLong(path, defaultValue);
        }
        return defaultValue;
    }

    double doubleValue(ConfigurationSection section, ConfigurationSection fallback, String path, double defaultValue) {
        if (section != null && section.contains(path)) {
            return section.getDouble(path, defaultValue);
        }
        if (fallback != null && fallback.contains(path)) {
            return fallback.getDouble(path, defaultValue);
        }
        return defaultValue;
    }

    boolean bool(ConfigurationSection section, ConfigurationSection fallback, String path, boolean defaultValue) {
        if (section != null && section.contains(path)) {
            return section.getBoolean(path, defaultValue);
        }
        if (fallback != null && fallback.contains(path)) {
            return fallback.getBoolean(path, defaultValue);
        }
        return defaultValue;
    }

    Vector vector(ConfigurationSection section, ConfigurationSection fallback, String path, String defaultValue) {
        return LocationUtils.parseVectorFromString(string(section, fallback, path, defaultValue));
    }

    Vector sectionVector(ConfigurationSection section, String path, Vector defaultValue) {
        ConfigurationSection vectorSection = child(section, path);
        if (vectorSection == null) {
            return defaultValue.clone();
        }
        return LocationUtils.getVectorFromSection(vectorSection);
    }

    Set<Material> materials(ConfigurationSection section, ConfigurationSection fallback, String path) {
        List<String> values;
        if (section != null && section.contains(path)) {
            values = section.getStringList(path);
        } else if (fallback != null) {
            values = fallback.getStringList(path);
        } else {
            values = List.of();
        }

        Set<Material> materials = new LinkedHashSet<>();
        for (String value : values) {
            Material material = Material.matchMaterial(value);
            if (material != null) {
                materials.add(material);
            }
        }
        return Collections.unmodifiableSet(materials);
    }

    Set<String> strings(ConfigurationSection section, ConfigurationSection fallback, String path) {
        List<String> values;
        if (section != null && section.contains(path)) {
            values = section.getStringList(path);
        } else if (fallback != null) {
            values = fallback.getStringList(path);
        } else {
            values = List.of();
        }

        Set<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                normalized.add(value.trim().toLowerCase(java.util.Locale.ROOT));
            }
        }
        return Collections.unmodifiableSet(normalized);
    }

    ConfigurationSection child(ConfigurationSection section, String path) {
        return section == null ? null : section.getConfigurationSection(path);
    }
}
