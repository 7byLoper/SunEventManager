package ru.loper.suneventmanager.api.models.settings;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Data;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import ru.loper.suneventmanager.SunEventManager;
import ru.loper.suneventmanager.utils.LocationUtils;

@Data
public class GeneratorSettings {
    protected final World eventWorld;
    protected final GenerateType generateType;

    protected final int generatorRadius;
    protected final Vector generateCenter;
    protected final int maxAttempts;
    protected final Vector locationOffset;

    protected final List<Biome> generatorBiomes;
    protected final BiomePropertyType biomesPropertyType;

    protected final int minY;
    protected final int maxY;
    protected final List<Material> bannedBlocks;
    protected final SurfaceCheckSettings floorCheck;
    protected final AirCheckSettings airCheck;
    protected final boolean requireEmptyRegion;

    protected final String staticLocationValue;

    public static GeneratorSettings fromSection(ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        final World eventWorld = unwrapWorld(section.getString("world"));
        final GenerateType generateType = GenerateType.getByName(section.getString("type"));

        final int generatorRadius = Math.max(0, section.getInt("radius"));
        final Vector generateCenter = LocationUtils.parseVectorFromString(section.getString("center", "0:90:0"));
        final int maxAttempts = Math.max(1, section.getInt("max_attempts", 128));
        final Vector locationOffset =
                LocationUtils.parseVectorFromString(section.getString("location_offset", "0:0:0"));

        final List<Biome> generatorBiomes = section.getStringList("biomes").stream()
                .map(GeneratorSettings::loadBiome)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        final BiomePropertyType biomesPropertyType =
                BiomePropertyType.getByName(section.getString("biomes_property_type"));

        final int defaultMinY = eventWorld == null ? -64 : eventWorld.getMinHeight();
        final int defaultMaxY = eventWorld == null ? 319 : eventWorld.getMaxHeight() - 1;
        final int minY = section.getInt("min_y", defaultMinY);
        final int maxY = section.getInt("max_y", defaultMaxY);

        final List<Material> bannedBlocks = section.getStringList("banned_blocks").stream()
                .map(GeneratorSettings::loadMaterial)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        final SurfaceCheckSettings floorCheck =
                SurfaceCheckSettings.fromSection(section.getConfigurationSection("check_floor"));
        final AirCheckSettings airCheck = AirCheckSettings.fromSection(section.getConfigurationSection("check_air"));
        final boolean requireEmptyRegion = section.getBoolean("require_empty_region", false);

        final String staticLocationValue = section.getString("static_location", "world:0:90:0");

        return new GeneratorSettings(
                eventWorld,
                generateType,
                generatorRadius,
                generateCenter,
                maxAttempts,
                locationOffset,
                generatorBiomes,
                biomesPropertyType,
                minY,
                maxY,
                bannedBlocks,
                floorCheck,
                airCheck,
                requireEmptyRegion,
                staticLocationValue);
    }

    private static @Nullable World unwrapWorld(String name) {
        return LocationUtils.resolveWorld(name);
    }

    private static @Nullable Biome loadBiome(String biomeName) {
        if (biomeName == null || biomeName.isBlank()) {
            return null;
        }
        try {
            return Biome.valueOf(normalizeBiomeName(biomeName));
        } catch (IllegalArgumentException e) {
            SunEventManager.getInstance().getLogger().warning("Неизвестный биом: " + biomeName);
            return null;
        }
    }

    private static @Nullable Material loadMaterial(String materialName) {
        if (materialName == null || materialName.isBlank()) {
            return null;
        }

        String normalizedName =
                switch (materialName.trim().toUpperCase(Locale.ROOT)) {
                    case "GRASS" -> "SHORT_GRASS";
                    default -> materialName.trim().toUpperCase(Locale.ROOT);
                };
        Material material = Material.matchMaterial(normalizedName);
        if (material == null) {
            SunEventManager.getInstance().getLogger().warning("Неизвестный материал генератора: " + materialName);
        }
        return material;
    }

    private static String normalizeBiomeName(String biomeName) {
        return switch (biomeName.toUpperCase(Locale.ROOT)) {
            case "SNOWY_TUNDRA" -> "SNOWY_PLAINS";
            case "MOUNTAINS" -> "WINDSWEPT_HILLS";
            case "MUSHROOM_FIELD_SHORE" -> "MUSHROOM_FIELDS";
            case "WOODED_BADLANDS_PLATEAU" -> "WOODED_BADLANDS";
            case "BADLANDS_PLATEAU" -> "BADLANDS";
            default -> biomeName.toUpperCase(Locale.ROOT);
        };
    }

    public boolean isAllowedBiome(Biome biome) {
        return biomesPropertyType.allowBiome(biome, generatorBiomes);
    }

    public boolean isAllowedHeight(int y) {
        return y >= minY && y <= maxY;
    }

    public boolean isBannedBlock(Material material) {
        return material != null && bannedBlocks.contains(material);
    }

    public enum GenerateType {
        STATIC,
        RANDOM;

        public static GenerateType getByName(String name) {
            try {
                return valueOf(name.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException | NullPointerException e) {
                return RANDOM;
            }
        }
    }

    public enum BiomePropertyType {
        BLACKLIST {
            @Override
            boolean allowBiome(Biome biome, List<Biome> biomes) {
                return !biomes.contains(biome);
            }
        },
        WHITELIST {
            @Override
            boolean allowBiome(Biome biome, List<Biome> biomes) {
                return biomes.contains(biome);
            }
        };

        public static BiomePropertyType getByName(String name) {
            try {
                return valueOf(name.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException | NullPointerException e) {
                return BLACKLIST;
            }
        }

        abstract boolean allowBiome(Biome biome, List<Biome> biomes);
    }

    public record SurfaceCheckSettings(boolean enabled, int radius, int step) {
        private static SurfaceCheckSettings fromSection(ConfigurationSection section) {
            if (section == null) {
                return new SurfaceCheckSettings(false, 0, 1);
            }

            return new SurfaceCheckSettings(
                    section.getBoolean("enabled", false),
                    Math.max(0, section.getInt("radius", 0)),
                    Math.max(1, section.getInt("step", 1)));
        }
    }

    public record AirCheckSettings(boolean enabled, int radius, int height, int step) {
        private static AirCheckSettings fromSection(ConfigurationSection section) {
            if (section == null) {
                return new AirCheckSettings(false, 0, 1, 1);
            }

            return new AirCheckSettings(
                    section.getBoolean("enabled", false),
                    Math.max(0, section.getInt("radius", 0)),
                    Math.max(1, section.getInt("height", 1)),
                    Math.max(1, section.getInt("step", 1)));
        }
    }
}
