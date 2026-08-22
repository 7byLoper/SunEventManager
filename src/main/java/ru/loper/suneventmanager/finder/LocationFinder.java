package ru.loper.suneventmanager.finder;

import io.papermc.lib.PaperLib;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.block.data.Waterlogged;
import ru.loper.suneventmanager.SunEventManager;
import ru.loper.suneventmanager.api.models.Event;
import ru.loper.suneventmanager.api.models.settings.GeneratorSettings;
import ru.loper.suneventmanager.api.models.settings.RegionSettings;
import ru.loper.suneventmanager.hook.WGHook;
import ru.loper.suneventmanager.utils.LocationUtils;

@RequiredArgsConstructor
public final class LocationFinder {
    private final Event event;
    private final GeneratorSettings generatorSettings;

    public void findLocation() {
        findLocation(1);
    }

    private void findLocation(int attempt) {
        if (!event.isActivate()) {
            return;
        }

        if (generatorSettings == null) {
            event.cancelLocationSearch("настройки генератора не загружены");
            return;
        }

        if (generatorSettings.getGenerateType() == GeneratorSettings.GenerateType.STATIC) {
            Location location = LocationUtils.parseLocationFromString(generatorSettings.getStaticLocationValue());
            if (location != null && location.getWorld() != null) {
                event.setEventLocation(applyLocationOffset(location));
                return;
            }

            event.cancelLocationSearch("не задана статическая локация или её мир");
            return;
        }

        World world = generatorSettings.getEventWorld();
        if (world == null) {
            event.cancelLocationSearch("мир для генератора не найден");
            return;
        }

        if (generatorSettings.getMinY() > generatorSettings.getMaxY()) {
            event.cancelLocationSearch("min_y больше max_y");
            return;
        }

        if (generatorSettings.getBiomesPropertyType() == GeneratorSettings.BiomePropertyType.WHITELIST
                && generatorSettings.getGeneratorBiomes().isEmpty()) {
            event.cancelLocationSearch("белый список биомов пуст или содержит только неизвестные биомы");
            return;
        }

        if (attempt > generatorSettings.getMaxAttempts()) {
            event.cancelLocationSearch(
                    "за " + generatorSettings.getMaxAttempts() + " попыток не найдено безопасное место");
            return;
        }

        CoordinateRange xRange = getCoordinateRange(world, true);
        CoordinateRange zRange = getCoordinateRange(world, false);
        if (xRange == null || zRange == null) {
            event.cancelLocationSearch("радиус генератора не пересекается с границей мира");
            return;
        }

        int x = getRandom(xRange);
        int z = getRandom(zRange);

        PaperLib.getChunkAtAsync(world, x >> 4, z >> 4, true)
                .whenComplete((chunk, throwable) -> Bukkit.getScheduler().runTask(SunEventManager.getInstance(), () -> {
                    if (!event.isActivate()) {
                        return;
                    }

                    if (throwable != null) {
                        findLocation(attempt + 1);
                        return;
                    }

                    Location location = new Location(world, x, world.getHighestBlockYAt(x, z) + 1, z);
                    if (isValidRandomLocation(location)) {
                        event.setEventLocation(applyLocationOffset(location));
                        return;
                    }

                    findLocation(attempt + 1);
                }));
    }

    private boolean isValidRandomLocation(Location location) {
        if (location.getWorld() == null) {
            return false;
        }

        Block surface = location.clone().subtract(0.0D, 1.0D, 0.0D).getBlock();
        if (!generatorSettings.isAllowedHeight(surface.getY())) {
            return false;
        }
        if (!generatorSettings.isAllowedBiome(surface.getBiome())) {
            return false;
        }
        if (surface.getType().isAir() || generatorSettings.isBannedBlock(surface.getType())) {
            return false;
        }
        if (hasLiquidAtSurface(surface)) {
            return false;
        }
        if (!isFloorValid(surface)) {
            return false;
        }
        if (!isAirValid(surface)) {
            return false;
        }
        return isRegionAvailable(applyLocationOffset(location));
    }

    private boolean hasLiquidAtSurface(Block surface) {
        World world = surface.getWorld();
        int minY = Math.max(world.getMinHeight(), surface.getY() - 3);
        for (int y = surface.getY(); y >= minY; y--) {
            Block block = world.getBlockAt(surface.getX(), y, surface.getZ());
            if (block.isLiquid()
                    || (block.getBlockData() instanceof Waterlogged waterlogged && waterlogged.isWaterlogged())) {
                return true;
            }
            if (block.getType().isSolid()) {
                return false;
            }
        }
        return false;
    }

    private boolean isFloorValid(Block surface) {
        GeneratorSettings.SurfaceCheckSettings check = generatorSettings.getFloorCheck();
        if (check == null || !check.enabled()) {
            return true;
        }

        World world = surface.getWorld();
        for (int x = surface.getX() - check.radius(); x <= surface.getX() + check.radius(); x += check.step()) {
            for (int z = surface.getZ() - check.radius(); z <= surface.getZ() + check.radius(); z += check.step()) {
                Block checked = world.getBlockAt(x, surface.getY(), z);
                if (checked.getType().isAir() || generatorSettings.isBannedBlock(checked.getType())) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isAirValid(Block surface) {
        GeneratorSettings.AirCheckSettings check = generatorSettings.getAirCheck();
        if (check == null || !check.enabled()) {
            return true;
        }

        World world = surface.getWorld();
        int y = surface.getY() + check.height();
        if (y >= world.getMaxHeight()) {
            return false;
        }

        for (int x = surface.getX() - check.radius(); x <= surface.getX() + check.radius(); x += check.step()) {
            for (int z = surface.getZ() - check.radius(); z <= surface.getZ() + check.radius(); z += check.step()) {
                if (!world.getBlockAt(x, y, z).getType().isAir()) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isRegionAvailable(Location location) {
        if (!generatorSettings.isRequireEmptyRegion()) {
            return true;
        }
        if (event.getEventRarity() == null) {
            return false;
        }

        RegionSettings regionSettings = event.getEventRarity().getRegionSettings();
        if (regionSettings == null || !regionSettings.isEnable()) {
            return true;
        }
        return WGHook.isRegionEmpty(location, regionSettings.getRegionVector());
    }

    private Location applyLocationOffset(Location location) {
        if (location == null) {
            return null;
        }
        return location.clone().add(generatorSettings.getLocationOffset());
    }

    private CoordinateRange getCoordinateRange(World world, boolean xAxis) {
        double center = xAxis
                ? generatorSettings.getGenerateCenter().getX()
                : generatorSettings.getGenerateCenter().getZ();
        double radius = generatorSettings.getGeneratorRadius();
        WorldBorder border = world.getWorldBorder();
        double borderCenter =
                xAxis ? border.getCenter().getX() : border.getCenter().getZ();
        double borderRadius = border.getSize() / 2.0D;

        int min = (int) Math.ceil(Math.max(center - radius, borderCenter - borderRadius));
        int max = (int) Math.floor(Math.min(center + radius, borderCenter + borderRadius));
        return min <= max ? new CoordinateRange(min, max) : null;
    }

    private int getRandom(CoordinateRange range) {
        return ThreadLocalRandom.current().nextInt(range.min(), range.max() + 1);
    }

    private record CoordinateRange(int min, int max) {}
}
