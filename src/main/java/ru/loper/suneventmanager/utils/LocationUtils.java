package ru.loper.suneventmanager.utils;

import java.util.Arrays;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.Vector;

@UtilityClass
public class LocationUtils {

    public Vector parseVectorFromString(String vectorString) {
        if (vectorString == null || vectorString.isBlank()) {
            return new Vector();
        }

        String[] values = vectorString.split(":", -1);
        return new Vector(parseDouble(values, 0), parseDouble(values, 1), parseDouble(values, 2));
    }

    public Location parseLocationFromString(String locationString) {
        if (locationString == null || locationString.isBlank()) {
            return null;
        }

        String[] values = locationString.split(":", -1);
        int coordinatesStart = values.length - 3;
        if (coordinatesStart < 1) {
            return null;
        }

        String worldName = String.join(":", Arrays.copyOfRange(values, 0, coordinatesStart))
                .trim();
        World world = resolveWorld(worldName);
        return new Location(
                world,
                parseDouble(values, coordinatesStart),
                parseDouble(values, coordinatesStart + 1),
                parseDouble(values, coordinatesStart + 2));
    }

    public World resolveWorld(String worldName) {
        if (worldName == null || worldName.isBlank()) {
            return null;
        }

        String normalizedName = worldName.trim();
        World world = Bukkit.getWorld(normalizedName);
        if (world != null) {
            return world;
        }

        NamespacedKey key = NamespacedKey.fromString(normalizedName);
        if (key == null && !normalizedName.contains(":")) {
            key = NamespacedKey.minecraft(normalizedName);
        }
        return key == null ? null : Bukkit.getWorld(key);
    }

    public Location getLocationFromSection(ConfigurationSection locationSection) {
        if (locationSection == null) {
            return null;
        }

        World world = resolveWorld(locationSection.getString("world", "world"));
        return new Location(
                world,
                locationSection.getDouble("x", 0),
                locationSection.getDouble("y", 0),
                locationSection.getDouble("z", 0));
    }

    public Vector getVectorFromSection(ConfigurationSection vectorSection) {
        if (vectorSection == null) {
            return new Vector();
        }

        return new Vector(
                vectorSection.getDouble("x", 0), vectorSection.getDouble("y", 0), vectorSection.getDouble("z", 0));
    }

    private double parseDouble(String[] values, int index) {
        if (index >= values.length || values[index].isBlank()) {
            return 0.0D;
        }

        try {
            return Double.parseDouble(values[index].trim());
        } catch (NumberFormatException ignored) {
            return 0.0D;
        }
    }
}
