package ru.loper.suneventmanager.commands.vote.impl;

import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;
import ru.loper.suncore.api.command.SubCommand;
import ru.loper.suneventmanager.api.modules.summoner.Summoner;
import ru.loper.suneventmanager.config.PluginConfigManager;

import java.util.List;

@RequiredArgsConstructor
public class StartSubCommand implements SubCommand {
    private final Summoner summoner;
    private final PluginConfigManager configManager;

    @Override
    public void onCommand(CommandSender commandSender, String[] args) {

    }

    @Override
    public List<String> onTabCompleter(CommandSender commandSender, String[] args) {
        return List.of();
    }
}
