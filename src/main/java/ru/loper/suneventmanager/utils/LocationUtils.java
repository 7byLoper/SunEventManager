package ru.loper.suneventmanager.utils;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.Vector;

public class LocationUtils {
    private LocationUtils() {
        //Утилитный класс
    }

    public static Vector getVectorFromSection(ConfigurationSection vectorSection) {
        if (vectorSection == null) {
            return new Vector(0, 0, 0);
        }

        double offsetX = vectorSection.getDouble("x", 0);
        double offsetY = vectorSection.getDouble("y", 0);
        double offsetZ = vectorSection.getDouble("z", 0);

        return new Vector(offsetX, offsetY, offsetZ);
    }
}
