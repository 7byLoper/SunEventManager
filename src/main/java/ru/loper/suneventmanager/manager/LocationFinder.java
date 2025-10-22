package ru.loper.suneventmanager.utils;

import io.papermc.lib.PaperLib;
import lombok.SneakyThrows;
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

        int x = getRandom(-generateRadius, generateRadius);
        int z = getRandom(-generateRadius, generateRadius);

        if (SunEventManager.isDebug()) {
            System.out.println("GET: X:" + x + " Z: " + z);
        }

        PaperLib.getChunkAtAsync(world, x, z, true).thenAcceptAsync(chunk -> {
            Location location = new Location(world, x, world.getHighestBlockYAt(x, z) + 1, z);

            if (configManager.isAllowedBiome(location.getBlock().getBiome())) {
                System.out.println("Successfully find location! " + location);
                event.setEventLocation(location);
            }

            if (SunEventManager.isDebug()) {
                System.out.println("Location find! Not allowed biome " + location);
            }

            findLocation();
        });
    }

    private int getRandom(int min, int max) {
        return ThreadLocalRandom.current().nextInt(max - min) + 2 + min;
    }
}