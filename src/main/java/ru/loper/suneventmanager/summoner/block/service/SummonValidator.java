package ru.loper.suneventmanager.summoner.block.service;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import ru.loper.suneventmanager.SunEventManager;
import ru.loper.suneventmanager.hook.WGHook;
import ru.loper.suneventmanager.summoner.block.model.SummonBlockDefinition;
import ru.loper.suneventmanager.summoner.block.model.SummonChecks;

public final class SummonValidator {

    public ValidationResult validate(
            Player player, Location placedLocation, Material supportBlock, SummonBlockDefinition definition) {
        if (player == null || placedLocation == null || placedLocation.getWorld() == null || definition == null) {
            return ValidationResult.INVALID_LOCATION;
        }

        SummonChecks checks = definition.checks();
        if (!isAllowedWorld(placedLocation.getWorld(), checks)) {
            return ValidationResult.WORLD;
        }

        int y = placedLocation.getBlockY();
        if (y < checks.minY()) {
            return ValidationResult.MIN_Y;
        }
        if (y > checks.maxY()) {
            return ValidationResult.MAX_Y;
        }
        if (supportBlock != null && checks.blockedBlocks().contains(supportBlock)) {
            return ValidationResult.BLOCKED_BLOCK;
        }
        if (checks.requireClearSky() && !hasClearSky(placedLocation)) {
            return ValidationResult.SKY;
        }
        if (checks.requireFlatSurface() && !hasFlatSurface(placedLocation, checks)) {
            return ValidationResult.SURFACE;
        }
        if (checks.minimumEventDistance() > 0.0D && isNearAnotherEvent(placedLocation, checks.minimumEventDistance())) {
            return ValidationResult.NEAR_EVENT;
        }
        if (checks.requireEmptyRegion()
                && !hasPermission(player, definition.regionBypassPermission())
                && !WGHook.isRegionEmpty(placedLocation, checks.regionSize())) {
            return ValidationResult.REGION;
        }
        return ValidationResult.OK;
    }

    public ValidationResult validateEventLocation(
            Player player, Location eventLocation, SummonBlockDefinition definition) {
        if (player == null || eventLocation == null || eventLocation.getWorld() == null || definition == null) {
            return ValidationResult.INVALID_LOCATION;
        }

        SummonChecks checks = definition.checks();
        int y = eventLocation.getBlockY();
        if (y < checks.minY()) {
            return ValidationResult.MIN_Y;
        }
        if (y > checks.maxY()) {
            return ValidationResult.MAX_Y;
        }
        if (checks.minimumEventDistance() > 0.0D && isNearAnotherEvent(eventLocation, checks.minimumEventDistance())) {
            return ValidationResult.NEAR_EVENT;
        }
        if (checks.requireEmptyRegion()
                && !hasPermission(player, definition.regionBypassPermission())
                && !WGHook.isRegionEmpty(eventLocation, checks.regionSize())) {
            return ValidationResult.REGION;
        }
        return ValidationResult.OK;
    }

    private boolean isAllowedWorld(World world, SummonChecks checks) {
        return checks.allowedWorlds().isEmpty()
                || checks.allowedWorlds().contains(world.getName().toLowerCase(java.util.Locale.ROOT));
    }

    private boolean hasClearSky(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return false;
        }

        for (int y = location.getBlockY() + 1; y < world.getMaxHeight(); y++) {
            if (!world.getBlockAt(location.getBlockX(), y, location.getBlockZ())
                    .getType()
                    .isAir()) {
                return false;
            }
        }
        return true;
    }

    private boolean hasFlatSurface(Location placedLocation, SummonChecks checks) {
        World world = placedLocation.getWorld();
        if (world == null) {
            return false;
        }

        int radius = Math.max(0, checks.flatSurfaceRadius());
        int baseY = placedLocation.getBlockY() - 1;
        int maxDelta = Math.max(0, checks.maximumSurfaceDelta());
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                int surfaceY = x == 0 && z == 0
                        ? baseY
                        : world.getHighestBlockYAt(placedLocation.getBlockX() + x, placedLocation.getBlockZ() + z);
                if (Math.abs(surfaceY - baseY) > maxDelta) {
                    return false;
                }
                if (!world.getBlockAt(placedLocation.getBlockX() + x, surfaceY, placedLocation.getBlockZ() + z)
                        .getType()
                        .isSolid()) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isNearAnotherEvent(Location location, double minimumDistance) {
        var manager = SunEventManager.getInstance() == null
                ? null
                : SunEventManager.getInstance().getEventManager();
        if (manager == null) {
            return false;
        }
        double maximumDistanceSquared = minimumDistance * minimumDistance;
        for (var event : manager.getActiveEvents()) {
            Location other = event.getEventLocation();
            if (other == null || other.getWorld() != location.getWorld()) {
                continue;
            }
            double dx = other.getX() - location.getX();
            double dz = other.getZ() - location.getZ();
            if (dx * dx + dz * dz < maximumDistanceSquared) {
                return true;
            }
        }
        return false;
    }

    private boolean hasPermission(Player player, String permission) {
        return permission != null && !permission.isBlank() && player.hasPermission(permission);
    }

    public enum ValidationResult {
        OK,
        MIN_Y,
        MAX_Y,
        BLOCKED_BLOCK,
        WORLD,
        SURFACE,
        NEAR_EVENT,
        SKY,
        REGION,
        INVALID_LOCATION
    }
}
