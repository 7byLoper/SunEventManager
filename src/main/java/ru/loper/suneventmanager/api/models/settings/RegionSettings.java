package ru.loper.suneventmanager.api.models.settings;

import com.sk89q.worldguard.protection.flags.Flag;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.Vector;
import ru.loper.suneventmanager.hook.WGHook;
import ru.loper.suneventmanager.utils.LocationUtils;

@Data
public class RegionSettings {
    private final boolean enable;
    private final Vector regionVector;
    private final Map<Flag<?>, String> regionFlags;

    public static RegionSettings fromSection(ConfigurationSection section) {
        if (section == null) {
            return new RegionSettings(false, new Vector(0, 0, 0), new HashMap<>());
        }

        final boolean enable = section.getBoolean("enable");
        final Vector regionVector = LocationUtils.parseVectorFromString(section.getString("size"));
        final Map<Flag<?>, String> regionFlags = WGHook.parseFlags(section.getConfigurationSection("flags"));

        return new RegionSettings(enable, regionVector, regionFlags);
    }
}
