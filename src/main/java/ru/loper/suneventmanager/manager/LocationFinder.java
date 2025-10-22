package ru.loper.suneventmanager.manager;

import io.papermc.lib.PaperLib;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import ru.loper.suneventmanager.SunEventManager;
import ru.loper.suneventmanager.api.modules.event.Event;
import ru.loper.suneventmanager.config.EventConfigManager;

import java.util.concurrent.ThreadLocalRandom;

public final class LocationFinder {

    private final Event event;

    public LocationFinder(Event event) {
        this.event = event;
    }

    @SneakyThrows
    public void findLocation() {
        EventConfigManager configManager = event.getConfigManager();
        int generateRadius = configManager.getGeneratorRadius();
        World world = configManager.getEventWorld();
        double worldRadius = world.getWorldBorder().getSize() / 2;

        if (generateRadius > worldRadius) {
            generateRadius = (int) worldRadius;
        }

        int x = getRandom(-generateRadius, generateRadius);
        int z = getRandom(-generateRadius, generateRadius);

        if (SunEventManager.isDebug()) {
            Bukkit.getLogger().info("GET: X:" + x + " Z: " + z);
        }

        PaperLib.getChunkAtAsync(world, x, z, true).thenAcceptAsync(chunk -> {
            Location location = new Location(world, x, world.getHighestBlockYAt(x, z) + 1, z);

            if (configManager.isAllowedBiome(location.getBlock().getBiome())) {
                if (SunEventManager.isDebug()) {
                    Bukkit.getLogger().info("Successfully find location! " + location);
                }
                event.setEventLocation(location);
                return;
            }

            if (SunEventManager.isDebug()) {
                Bukkit.getLogger().info("Location find! Not allowed biome " + location);
            }

            findLocation();
        });
    }

    private int getRandom(int min, int max) {
        return ThreadLocalRandom.current().nextInt(max - min) + 2 + min;
    }
}