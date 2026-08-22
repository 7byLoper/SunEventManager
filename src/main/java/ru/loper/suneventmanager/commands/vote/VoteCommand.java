package ru.loper.suneventmanager.commands.vote;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.loper.suncore.api.command.CommandServices;
import ru.loper.suncore.api.command.executor.BaseCommandExecutor;
import ru.loper.suncore.api.command.register.CommandRegister;
import ru.loper.suneventmanager.SunEventManager;
import ru.loper.suneventmanager.api.models.settings.CommandSettings;
import ru.loper.suneventmanager.commands.vote.impl.StartSubCommand;
import ru.loper.suneventmanager.commands.vote.impl.StopSubCommand;
import ru.loper.suneventmanager.manager.VoteManager;

@CommandRegister(name = "vote", permission = "suneventmanager.command.vote")
public class VoteCommand extends BaseCommandExecutor {
    private final SunEventManager plugin;

    public VoteCommand(SunEventManager plugin) {
        this.plugin = plugin;

        CommandSettings settings = plugin.getConfigManager().getVoteCommandSettings();
        if (settings.isEnable()
                && settings.isStandalone()
                && !settings.getCommand().isBlank()) {
            CommandServices.registrar().registerCommand(settings.getCommand(), this, plugin);
        }
    }

    @Override
    public String getNoPermissionMessage() {
        return plugin.getMessageService()
                .toLegacy(null, plugin.getConfigManager().getNoPermissionMessage(), java.util.Collections.emptyMap());
    }

    @Override
    public void registerWrappers() {
        addSubCommand(new StartSubCommand(plugin.getSummoner().getVoteManager(), plugin.getConfigManager()));
        addSubCommand(new StopSubCommand(plugin.getSummoner().getVoteManager(), plugin.getConfigManager()));
    }

    @Override
    public void handleNoArguments(@NotNull CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return;
        }

        VoteManager voteManager = plugin.getSummoner().getVoteManager();
        if (!voteManager.isVoteInProgress()) {
            plugin.getMessageService()
                    .send(player, plugin.getConfigManager().getVoteIsStopMessage(), java.util.Collections.emptyMap());
            return;
        }

        voteManager.getCurrentVote().getVoteMenu().show(player);
    }
}
