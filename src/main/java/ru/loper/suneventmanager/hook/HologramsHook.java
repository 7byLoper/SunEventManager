package ru.loper.suneventmanager.hook;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import ru.loper.suncore.utils.Colorize;

import java.util.List;

public class HologramsHook {
    public static void createOrUpdateHologram(@NotNull List<String> lines, @NotNull Location location, @NotNull String name) {
        Hologram hologram = DHAPI.getHologram(name);

        if (hologram != null) {
            if (!hologram.getLocation().equals(location)) {
                hologram.setLocation(location);
                hologram.realignLines();
            }

            DHAPI.setHologramLines(hologram, Colorize.parse(lines));
            return;
        }

        DHAPI.createHologram(name, location, Colorize.parse(lines));
    }

    public static void remove(@NotNull String name) {
        Hologram hologram = DHAPI.getHologram(name);
        if (hologram != null) {
            DHAPI.removeHologram(name);
        }
    }
}
