package ru.loper.suneventmanager.commands.event;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;
import ru.loper.suncore.api.command.AdvancedSmartCommandExecutor;
import ru.loper.suneventmanager.SunEventManager;
import ru.loper.suneventmanager.api.modules.event.Event;
import ru.loper.suneventmanager.commands.event.impl.SettingsSubCommand;
import ru.loper.suneventmanager.commands.event.impl.StartSubCommand;
import ru.loper.suneventmanager.commands.event.impl.StopSubCommand;
import ru.loper.suneventmanager.config.PluginConfigManager;

import java.util.List;

public class EventCommand extends AdvancedSmartCommandExecutor {
    private final PluginConfigManager configManager;
    private final SunEventManager plugin;

    public EventCommand(SunEventManager plugin) {
        configManager = plugin.getConfigManager();
        this.plugin = plugin;
        addSubCommand(new SettingsSubCommand(configManager), new Permission("suneventsmanager.command.events.settings"), "settings");
        addSubCommand(new StopSubCommand(plugin.getEventManager()), new Permission("suneventsmanager.command.events.stop"), "stop");
        addSubCommand(new StartSubCommand(plugin.getEventManager()), new Permission("suneventsmanager.command.events.start"), "start");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            boolean isVoteInProgress = plugin.getSummoner().getVoteManager().isVoteInProgress();

            if (isVoteInProgress) {
                configManager.getVoteStartMessage().forEach(sender::sendMessage);
            }

            List<Event> activeEvents = plugin.getEventManager().getActiveEvents()
                    .stream()
                    .filter(Event::isActivate)
                    .toList();

            if (activeEvents.isEmpty()) {
                if (!isVoteInProgress) {
                    sender.sendMessage(plugin.getSummoner().getEventStartTimeMessage());
                }
                return true;
            }

            activeEvents.forEach(event -> sender.sendMessage(event.getTimerMessage()));
            return true;
        }

        SubCommandWrapper subCommand = getCommandByLabel(args[0]);
        if (subCommand == null) {
            return true;
        }

        if (!sender.hasPermission(subCommand.getPermission())) {
            sender.sendMessage(getDontPermissionMessage());
            return true;
        }

        subCommand.getCommand().onCommand(sender, args);
        return true;
    }

    @Override
    public String getDontPermissionMessage() {
        return configManager.getNoPermissionMessage();
    }
}
