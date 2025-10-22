package ru.loper.suneventmanager.config;

import org.bukkit.plugin.Plugin;
import ru.loper.suncore.api.config.ConfigManager;

public class EventsConfigManager extends ConfigManager {
    public EventsConfigManager(Plugin plugin) {
        super(plugin);
    }

    @Override
    public void loadConfigs() {
        plugin.saveDefaultConfig();
    }

    @Override
    public void loadValues() {

    }
}
