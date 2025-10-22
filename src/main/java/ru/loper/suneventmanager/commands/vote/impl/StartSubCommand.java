package ru.loper.suneventmanager.commands.vote.impl;

import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;
import ru.loper.suncore.api.command.SubCommand;
import ru.loper.suncore.utils.Colorize;
import ru.loper.suneventmanager.api.modules.vote.VoteManager;
import ru.loper.suneventmanager.config.PluginConfigManager;

import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
public class StartSubCommand implements SubCommand {
    private final VoteManager voteManager;
    private final PluginConfigManager configManager;

    @Override
    public void onCommand(CommandSender commandSender, String[] args) {
        if (voteManager.isVoteInProgress()) {
            commandSender.sendMessage(configManager.getVoteIsStartMessage());
            return;
        }

        voteManager.startVote();
        commandSender.sendMessage(Colorize.parse("&b▶ &fВы успешно &6запустили&f голосование"));
    }

    @Override
    public List<String> onTabCompleter(CommandSender commandSender, String[] args) {
        return Collections.emptyList();
    }
}
