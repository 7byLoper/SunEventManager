package ru.loper.suneventmanager.commands.event.impl;

import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.loper.suncore.api.command.BuildableCommand;
import ru.loper.suncore.api.command.register.SubCommandRegister;
import ru.loper.suneventmanager.SunEventManager;
import ru.loper.suneventmanager.config.PluginConfigManager;
import ru.loper.suneventmanager.manager.VoteManager;
import ru.loper.suneventmanager.message.ConfiguredMessages;

@RequiredArgsConstructor
@SubCommandRegister(permission = "suneventmanager.command.vote", aliases = "vote")
public class EventVoteSubCommand implements BuildableCommand {
    private final SunEventManager plugin;

    @Override
    public void handle(@NotNull CommandSender sender, String[] args) {
        if (args.length <= 1) {
            openVote(sender);
            return;
        }

        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "start" -> startVote(sender);
            case "stop" -> stopVote(sender);
            default -> openVote(sender);
        }
    }

    private void openVote(CommandSender sender) {
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

    private void startVote(CommandSender sender) {
        if (!sender.hasPermission("suneventmanager.command.vote.start")) {
            plugin.getMessageService()
                    .send(sender, plugin.getConfigManager().getNoPermissionMessage(), java.util.Collections.emptyMap());
            return;
        }

        VoteManager voteManager = plugin.getSummoner().getVoteManager();
        PluginConfigManager config = plugin.getConfigManager();
        if (voteManager.isVoteInProgress()) {
            plugin.getMessageService().send(sender, config.getVoteIsStartMessage(), java.util.Collections.emptyMap());
            return;
        }

        voteManager.startVote();
        if (voteManager.isVoteInProgress()) {
            ConfiguredMessages.send(sender, "commands.vote.started", "&b▶ &fВы успешно &6запустили&f голосование");
        } else {
            ConfiguredMessages.send(
                    sender,
                    "commands.vote.start_failed",
                    "&c▶ &fНе удалось запустить голосование: нет доступных ивентов.");
        }
    }

    private void stopVote(CommandSender sender) {
        if (!sender.hasPermission("suneventmanager.command.vote.stop")) {
            plugin.getMessageService()
                    .send(sender, plugin.getConfigManager().getNoPermissionMessage(), java.util.Collections.emptyMap());
            return;
        }

        VoteManager voteManager = plugin.getSummoner().getVoteManager();
        if (!voteManager.isVoteInProgress()) {
            plugin.getMessageService()
                    .send(sender, plugin.getConfigManager().getVoteIsStopMessage(), java.util.Collections.emptyMap());
            return;
        }

        voteManager.clearCurrentVote();
        ConfiguredMessages.send(sender, "commands.vote.stopped", "&b▶ &fВы успешно &6остановили&f голосование");
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, String[] args) {
        if (args.length == 2) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return List.of("start", "stop").stream()
                    .filter(option -> option.startsWith(prefix))
                    .toList();
        }
        return List.of();
    }
}
