package ru.loper.suneventmanager.hook;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.*;
import com.sk89q.worldguard.protection.managers.RemovalStrategy;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import ru.loper.suneventmanager.SunEventManager;
import ru.loper.suneventmanager.api.modules.event.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RegionManager {

    public static boolean isRegionEmpty(Location location, Vector regionVector) {
        double x = regionVector.getX();
        double y = regionVector.getY();
        double z = regionVector.getZ();

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        com.sk89q.worldguard.protection.managers.RegionManager regions = container.get(BukkitAdapter.adapt(location.getWorld()));

        Location point1 = new Location(location.getWorld(), location.getX() + x, location.getY() + y, location.getZ() + z);
        Location point2 = new Location(location.getWorld(), location.getX() - x, location.getY() - y, location.getZ() - z);

        ProtectedCuboidRegion region = new ProtectedCuboidRegion(UUID.randomUUID() + "_region",
                BlockVector3.at(point1.getX(), point1.getY(), point1.getZ()),
                BlockVector3.at(point2.getX(), point2.getY(), point2.getZ()));

        assert regions != null;
        Map<String, ProtectedRegion> rg = regions.getRegions();

        List<ProtectedRegion> candidates = new ArrayList<>(rg.values());
        List<ProtectedRegion> overlapping = region.getIntersectingRegions(candidates);

        return overlapping.isEmpty();
    }

    public static void create(Event event) {
        try {
            double x = event.getConfigManager().getRegionRadius().getX();
            double y = event.getConfigManager().getRegionRadius().getY();
            double z = event.getConfigManager().getRegionRadius().getZ();
            Location location = event.getEventLocation();

            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            com.sk89q.worldguard.protection.managers.RegionManager regions = container.get(BukkitAdapter.adapt(location.getWorld()));

            Location point1 = new Location(location.getWorld(), location.getX() + x, location.getY() + y, location.getZ() + z);
            Location point2 = new Location(location.getWorld(), location.getX() - x, location.getY() - y, location.getZ() - z);

            ProtectedCuboidRegion region = new ProtectedCuboidRegion("sunevent_" + event.getId() + "_region_" + event.getUuid(),
                    BlockVector3.at(point1.getX(), point1.getY(), point1.getZ()),
                    BlockVector3.at(point2.getX(), point2.getY(), point2.getZ()));

            setFlags(event, region);

            assert regions != null;
            regions.addRegion(region);
        } catch (Exception e) {
            SunEventManager.getInstance().getLogger().severe("Error create region: event - " + event.getId() + ", uuid - " + event.getUuid());
        }
    }

    private static void setFlags(Event event, ProtectedCuboidRegion region) throws InvalidFlagFormat {
        Map<Flag<?>, String> flags = event.getEventRarity().getFlags();

        for (Map.Entry<Flag<?>, String> entry : flags.entrySet()) {
            Flag<?> flag = entry.getKey();
            String value = entry.getValue();

            setFlag(flag, region, value);
        }
    }

    private static void setFlag(Flag<?> flag, ProtectedCuboidRegion region, String value) throws InvalidFlagFormat {
        if (flag instanceof StateFlag stateFlag) {
            region.setFlag(
                    stateFlag,
                    stateFlag.parseInput(FlagContext.create().setInput(value.toUpperCase()).build())
            );
        } else if (flag instanceof StringFlag stringFlag) {
            region.setFlag(
                    stringFlag,
                    value
            );
        } else if (flag instanceof BooleanFlag booleanFlag) {
            region.setFlag(
                    booleanFlag,
                    booleanFlag.parseInput(FlagContext.create().setInput(value).build())
            );
        } else if (flag instanceof DoubleFlag doubleFlag) {
            region.setFlag(
                    doubleFlag,
                    doubleFlag.parseInput(FlagContext.create().setInput(value).build())

            );
        } else if (flag instanceof IntegerFlag intFlag) {
            region.setFlag(
                    intFlag,
                    intFlag.parseInput(FlagContext.create().setInput(value).build())
            );
        }
    }

    public static void remove(Event event) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        com.sk89q.worldguard.protection.managers.RegionManager regions = container.get(BukkitAdapter.adapt(event.getEventLocation().getWorld()));
        assert regions != null;
        regions.removeRegion("sunevent_" + event.getId() + "_region_" + event.getUuid(), RemovalStrategy.REMOVE_CHILDREN);
    }
}
