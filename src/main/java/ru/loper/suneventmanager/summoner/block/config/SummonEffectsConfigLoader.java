package ru.loper.suneventmanager.summoner.block.config;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import ru.loper.suneventmanager.summoner.block.model.SummonEffects;
import ru.loper.suneventmanager.summoner.block.model.SummonParticleSettings;
import ru.loper.suneventmanager.summoner.block.model.SummonSoundSettings;

final class SummonEffectsConfigLoader {
    private final Plugin plugin;
    private final SummonConfigValueReader values;

    SummonEffectsConfigLoader(Plugin plugin, SummonConfigValueReader values) {
        this.plugin = plugin;
        this.values = values;
    }

    SummonEffects load(ConfigurationSection section, ConfigurationSection fallback) {
        return new SummonEffects(
                loadSound(
                        values.child(section, "sounds.success"),
                        values.child(fallback, "sounds.success"),
                        false,
                        false),
                loadSound(
                        values.child(section, "sounds.failed"), values.child(fallback, "sounds.failed"), false, false),
                loadSound(values.child(section, "sounds.spawn"), values.child(fallback, "sounds.spawn"), false, true),
                loadParticles(values.child(section, "particles"), values.child(fallback, "particles")));
    }

    private SummonSoundSettings loadSound(
            ConfigurationSection section,
            ConfigurationSection fallback,
            boolean defaultEnabled,
            boolean defaultGlobal) {
        boolean enabled = values.bool(section, fallback, "enabled", defaultEnabled);
        Sound sound = parseSound(values.string(section, fallback, "sound", ""));
        float volume = (float) values.doubleValue(section, fallback, "volume", 1.0D);
        float pitch = (float) values.doubleValue(section, fallback, "pitch", 1.0D);
        boolean global = values.bool(section, fallback, "global", defaultGlobal);
        return new SummonSoundSettings(enabled && sound != null, sound, volume, pitch, global);
    }

    private List<SummonParticleSettings> loadParticles(ConfigurationSection section, ConfigurationSection fallback) {
        ConfigurationSection source = section != null ? section : fallback;
        if (source == null) {
            return List.of();
        }

        return source.getKeys(false).stream()
                .map(key -> loadParticle(source.getConfigurationSection(key), key))
                .filter(Objects::nonNull)
                .toList();
    }

    private SummonParticleSettings loadParticle(ConfigurationSection section, String id) {
        if (section == null) {
            return null;
        }

        Particle particle = parseParticle(section.getString("particle", ""));
        if (particle == null) {
            plugin.getLogger().warning("Summon particle '" + id + "' пропущен: неизвестный particle.");
            return null;
        }

        return new SummonParticleSettings(
                section.getBoolean("enabled", true),
                particle,
                Math.max(0, section.getInt("amount", 1)),
                section.getDouble("speed", 0.0D),
                values.sectionVector(section, "offset", new Vector()),
                values.sectionVector(section, "spread", new Vector()));
    }

    private Sound parseSound(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Sound.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            plugin.getLogger().warning("Неизвестный summon sound: " + value);
            return null;
        }
    }

    private Particle parseParticle(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        Particle direct = findParticle(normalized);
        if (direct != null) {
            return direct;
        }

        String alias =
                switch (normalized) {
                    case "SMOKE_NORMAL" -> "SMOKE";
                    case "SMOKE_LARGE" -> "LARGE_SMOKE";
                    case "SPELL_WITCH" -> "WITCH";
                    case "TOWN_AURA" -> "MYCELIUM";
                    case "EXPLOSION_HUGE" -> "EXPLOSION_EMITTER";
                    default -> normalized;
                };
        return alias.equals(normalized) ? null : findParticle(alias);
    }

    private Particle findParticle(String name) {
        try {
            return Particle.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
