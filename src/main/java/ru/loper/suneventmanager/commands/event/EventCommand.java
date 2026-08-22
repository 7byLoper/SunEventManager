package ru.loper.suneventmanager.commands.event;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import ru.loper.suncore.api.command.CommandServices;
import ru.loper.suncore.api.command.executor.BaseCommandExecutor;
import ru.loper.suncore.api.command.register.CommandRegister;
import ru.loper.suneventmanager.SunEventManager;
import ru.loper.suneventmanager.api.models.settings.CommandSettings;
import ru.loper.suneventmanager.commands.event.impl.*;
import ru.loper.suneventmanager.config.PluginConfigManager;

@CommandRegister(name = "event", permission = "suneventmanager.command.event")
public class EventCommand extends BaseCommandExecutor {
    private final PluginConfigManager configManager;
    private final SunEventManager plugin;

    public EventCommand(SunEventManager plugin) {
        this.configManager = plugin.getConfigManager();
        this.plugin = plugin;

        CommandSettings commandSettings = configManager.getEventCommandSettings();
        if (commandSettings.isEnable() && !commandSettings.getCommand().isBlank()) {
            CommandServices.registrar().registerCommand(commandSettings.getCommand(), this, plugin);
        }
    }

    @Override
    public String getNoPermissionMessage() {
        return plugin.getMessageService()
                .toLegacy(null, configManager.getNoPermissionMessage(), java.util.Collections.emptyMap());
    }

    @Override
    public void registerWrappers() {
        addSubCommand(new SettingsSubCommand(configManager));
        addSubCommand(new SetTimerSubCommand(plugin.getEventManager()));
        addSubCommand(new StopSubCommand(plugin.getEventManager()));
        addSubCommand(new StartSubCommand(plugin.getEventManager()));
        addSubCommand(new SummonSubCommand(plugin.getSummonBlockManager()));
        addSubCommand(new GpsSubCommand(plugin));

        if (configManager.isEventDelaySubcommandEnabled()) {
            addSubCommand(new DelaySubCommand(plugin));
        }

        CommandSettings voteSettings = configManager.getVoteCommandSettings();
        if (voteSettings.isEnable() && voteSettings.isEventSubcommand()) {
            addSubCommand(new EventVoteSubCommand(plugin));
        }
    }

    @Override
    public void handleNoArguments(@NotNull CommandSender sender) {
        if (configManager.getEventNoArgumentsAction() == PluginConfigManager.NoArgumentsAction.NONE) {
            return;
        }

        plugin.getMessageService()
                .sendLines(sender, configManager.getEventHelpMessage(), java.util.Collections.emptyMap());
    }
}
