package ru.loper.suneventmanager.commands.vote.impl;

import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import ru.loper.suncore.api.command.BuildableCommand;
import ru.loper.suncore.api.command.register.SubCommandRegister;
import ru.loper.suneventmanager.config.PluginConfigManager;
import ru.loper.suneventmanager.manager.VoteManager;
import ru.loper.suneventmanager.message.ConfiguredMessages;

@RequiredArgsConstructor
@SubCommandRegister(permission = "suneventmanager.command.vote.start", aliases = "start")
public class StartSubCommand implements BuildableCommand {
    private final VoteManager voteManager;
    private final PluginConfigManager configManager;

    @Override
    public void handle(@NotNull CommandSender commandSender, String[] args) {
        if (voteManager.isVoteInProgress()) {
            ru.loper.suneventmanager.SunEventManager.getInstance()
                    .getMessageService()
                    .send(commandSender, configManager.getVoteIsStartMessage(), java.util.Collections.emptyMap());
            return;
        }

        voteManager.startVote();
        if (voteManager.isVoteInProgress()) {
            ConfiguredMessages.send(
                    commandSender, "commands.vote.started", "&b▶ &fВы успешно &6запустили&f голосование");
        } else {
            ConfiguredMessages.send(
                    commandSender,
                    "commands.vote.start_failed",
                    "&c▶ &fНе удалось запустить голосование: нет доступных ивентов.");
        }
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender commandSender, String[] args) {
        return Collections.emptyList();
    }
}
