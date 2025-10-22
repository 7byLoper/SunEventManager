package ru.loper.suneventmanager.commands.vote;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;
import ru.loper.suncore.api.command.AdvancedSmartCommandExecutor;
import ru.loper.suneventmanager.SunEventManager;
import ru.loper.suneventmanager.api.modules.vote.VoteManager;
import ru.loper.suneventmanager.commands.vote.impl.StartSubCommand;
import ru.loper.suneventmanager.commands.vote.impl.StopSubCommand;

public class VoteCommand extends AdvancedSmartCommandExecutor {
    private final SunEventManager eventManager;

    public VoteCommand(SunEventManager eventManager) {
        this.eventManager = eventManager;
        addSubCommand(new StartSubCommand(eventManager.getSummoner().getVoteManager(), eventManager.getConfigManager()), new Permission("suneventmanger.command.vote.start"), "start");
        addSubCommand(new StopSubCommand(eventManager.getSummoner().getVoteManager(), eventManager.getConfigManager()), new Permission("suneventmanger.command.vote.stop"), "stop");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                VoteManager voteManager = eventManager.getSummoner().getVoteManager();
                if (!voteManager.isVoteInProgress()) {
                    player.sendMessage(eventManager.getConfigManager().getVoteIsStopMessage());
                    return true;
                }
                voteManager.getCurrentVote().getVoteMenu().show(player);
                return true;
            }

            sender.sendMessage("Данная команда доступна только игрокам");
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
        return eventManager.getConfigManager().getNoPermissionMessage();
    }
}
