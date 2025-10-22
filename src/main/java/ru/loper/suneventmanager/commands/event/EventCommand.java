package ru.loper.suneventmanager.commands.event;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import ru.loper.suncore.api.command.AdvancedSmartCommandExecutor;

public class EventCommand extends AdvancedSmartCommandExecutor {

    public EventCommand() {
        //addSubCommand();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
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
        return "";
    }
}
