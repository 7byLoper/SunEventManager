package ru.loper.suneventmanager;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import ru.loper.suneventmanager.api.modules.event.EventManager;
import ru.loper.suneventmanager.api.modules.summoner.Summoner;
import ru.loper.suneventmanager.commands.event.EventCommand;
import ru.loper.suneventmanager.commands.vote.VoteCommand;
import ru.loper.suneventmanager.config.PluginConfigManager;
import ru.loper.suneventmanager.listeners.ChestListener;
import ru.loper.suneventmanager.listeners.VoteMenuListener;

import java.lang.reflect.Constructor;

@Getter
public final class SunEventManager extends JavaPlugin {
    @Getter
    private static SunEventManager instance;

    private PluginConfigManager configManager;
    private Summoner summoner;
    private EventManager eventManager;

    public static boolean isDebug() {
        return getInstance().configManager.isDebug();
    }

    @Override
    public void onEnable() {
        instance = this;

        configManager = new PluginConfigManager(this);
        summoner = new Summoner(configManager, this);
        eventManager = new EventManager(this);

        Bukkit.getPluginManager().registerEvents(new ChestListener(), this);
        Bukkit.getPluginManager().registerEvents(new VoteMenuListener(), this);

        initCommands();
    }

    @Override
    public void onDisable() {
        if (eventManager != null) {
            eventManager.stopAllActiveEvents();
            eventManager.clearAllRegisteredEvents();
        }
    }

    private void initCommands() {
        registerCommand(configManager.getEventCommand(), new EventCommand(this));
        registerCommand(configManager.getVoteCommand(), new VoteCommand(this));
    }

    private void registerCommand(String commandName, CommandExecutor executor) {
        try {
            CommandMap commandMap = getServer().getCommandMap();
            Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            constructor.setAccessible(true);

            PluginCommand command = constructor.newInstance(commandName, this);

            command.setExecutor(executor);
            commandMap.register(getDescription().getName(), command);
        } catch (Exception exception) {
            getLogger().severe("Unable to register command: " + commandName + ". Error: " + exception.getMessage());
        }
    }

}
