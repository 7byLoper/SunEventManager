package ru.loper.suneventmanager.hook;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.*;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.RemovalStrategy;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.Vector;
import ru.loper.suneventmanager.SunEventManager;
import ru.loper.suneventmanager.api.models.settings.RegionSettings;

@UtilityClass
public class WGHook {
    public boolean isRegionEmpty(Location location, Vector regionVector) {
        if (location == null || location.getWorld() == null || regionVector == null) {
            return false;
        }

        RegionManager regions;
        try {
            regions = getRegionManager(location.getWorld());
        } catch (LinkageError error) {
            logUnavailable(error);
            return false;
        }
        if (regions == null) {
            return false;
        }

        ProtectedCuboidRegion candidate = createCuboid(UUID.randomUUID() + "_region", location, regionVector);
        List<ProtectedRegion> existing = regions.getRegions().values().stream()
                .filter(region -> !"__global__".equalsIgnoreCase(region.getId()))
                .toList();
        return candidate.getIntersectingRegions(existing).isEmpty();
    }

    public Map<Flag<?>, String> parseFlags(ConfigurationSection flagsSection) {
        Map<Flag<?>, String> flagsMap = new HashMap<>();
        if (flagsSection == null) {
            return flagsMap;
        }

        try {
            for (String key : flagsSection.getKeys(false)) {
                Flag<?> wgFlag = WorldGuard.getInstance().getFlagRegistry().get(key);
                String value = flagsSection.getString(key);
                if (wgFlag != null && value != null) {
                    flagsMap.put(wgFlag, value);
                }
            }
        } catch (LinkageError error) {
            logUnavailable(error);
        }
        return flagsMap;
    }

    public void create(String name, RegionSettings regionSettings, Location location) {
        if (name == null || regionSettings == null || location == null || location.getWorld() == null) {
            return;
        }

        try {
            RegionManager regions = getRegionManager(location.getWorld());
            if (regions == null) {
                SunEventManager.getInstance()
                        .getLogger()
                        .warning("WorldGuard RegionManager недоступен для мира "
                                + location.getWorld().getName());
                return;
            }

            ProtectedCuboidRegion region = createCuboid(name, location, regionSettings.getRegionVector());
            setFlags(regionSettings.getRegionFlags(), region);
            regions.addRegion(region);
        } catch (Exception e) {
            SunEventManager.getInstance()
                    .getLogger()
                    .log(java.util.logging.Level.SEVERE, "Ошибка создания WorldGuard-региона: " + name, e);
        } catch (LinkageError error) {
            logUnavailable(error);
        }
    }

    private ProtectedCuboidRegion createCuboid(String name, Location location, Vector vector) {
        double x = Math.abs(vector.getX());
        double y = Math.abs(vector.getY());
        double z = Math.abs(vector.getZ());

        BlockVector3 min = BlockVector3.at(
                Math.floor(location.getX() - x), Math.floor(location.getY() - y), Math.floor(location.getZ() - z));
        BlockVector3 max = BlockVector3.at(
                Math.ceil(location.getX() + x), Math.ceil(location.getY() + y), Math.ceil(location.getZ() + z));
        return new ProtectedCuboidRegion(name, min, max);
    }

    private RegionManager getRegionManager(World world) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        return container.get(BukkitAdapter.adapt(world));
    }

    private void setFlags(Map<Flag<?>, String> flags, ProtectedCuboidRegion region) {
        for (Map.Entry<Flag<?>, String> entry : flags.entrySet()) {
            setFlag(entry.getKey(), region, entry.getValue());
        }
    }

    private void setFlag(Flag<?> flag, ProtectedCuboidRegion region, String value) {
        if (value == null) {
            return;
        }

        try {
            FlagContext context = FlagContext.create().setInput(value).build();
            if (flag instanceof StateFlag stateFlag) {
                region.setFlag(stateFlag, stateFlag.parseInput(context));
            } else if (flag instanceof StringFlag stringFlag) {
                region.setFlag(stringFlag, value);
            } else if (flag instanceof BooleanFlag booleanFlag) {
                region.setFlag(booleanFlag, booleanFlag.parseInput(context));
            } else if (flag instanceof DoubleFlag doubleFlag) {
                region.setFlag(doubleFlag, doubleFlag.parseInput(context));
            } else if (flag instanceof IntegerFlag integerFlag) {
                region.setFlag(integerFlag, integerFlag.parseInput(context));
            }
        } catch (InvalidFlagFormat exception) {
            SunEventManager.getInstance()
                    .getLogger()
                    .warning("Invalid WorldGuard flag value '" + value + "' for flag '" + flag.getName()
                            + "'. Flag skipped.");
        }
    }

    public void remove(World world, String name) {
        if (world == null || name == null) {
            return;
        }

        try {
            RegionManager regions = getRegionManager(world);
            if (regions != null) {
                regions.removeRegion(name, RemovalStrategy.REMOVE_CHILDREN);
            }
        } catch (LinkageError error) {
            logUnavailable(error);
        }
    }

    private void logUnavailable(LinkageError error) {
        SunEventManager.getInstance()
                .getLogger()
                .warning("WorldGuard недоступен, региональная защита события отключена: " + error.getMessage());
    }
}
