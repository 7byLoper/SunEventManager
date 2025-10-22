package ru.loper.suneventmanager.hook;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.FlagContext;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
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

public class WorldGuardHook {

    public static boolean isRegionEmpty(Location location, Vector regionVector) {
        double x = regionVector.getX();
        double y = regionVector.getY();
        double z = regionVector.getZ();

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(location.getWorld()));

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
            RegionManager regions = container.get(BukkitAdapter.adapt(location.getWorld()));

            Location point1 = new Location(location.getWorld(), location.getX() + x, location.getY() + y, location.getZ() + z);
            Location point2 = new Location(location.getWorld(), location.getX() - x, location.getY() - y, location.getZ() - z);

            ProtectedCuboidRegion region = new ProtectedCuboidRegion(event.getId() + "_region_" + event.getUuid(),
                    BlockVector3.at(point1.getX(), point1.getY(), point1.getZ()),
                    BlockVector3.at(point2.getX(), point2.getY(), point2.getZ()));

            Map<Flag<?>, String> flags = event.getConfigManager().getFlags();

            for (Map.Entry<Flag<?>, String> entry : flags.entrySet()) {
                StateFlag stateFlag = (StateFlag) entry.getKey();
                region.setFlag(stateFlag, stateFlag.parseInput(FlagContext.create().setInput((entry.getValue()).toUpperCase()).build()));
            }

            assert regions != null;
            regions.addRegion(region);
        } catch (Exception e) {
            SunEventManager.getInstance().getLogger().severe("Error create region: event - " + event.getId() + ", uuid - " + event.getUuid());
        }
    }

    public static void remove(Event event) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(event.getEventLocation().getWorld()));
        assert regions != null;
        regions.removeRegion(event.getId() + "_region_" + event.getUuid(), RemovalStrategy.REMOVE_CHILDREN);
    }
}
