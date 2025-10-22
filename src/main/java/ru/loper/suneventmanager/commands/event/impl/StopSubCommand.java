package ru.loper.suneventmanager.commands.event.impl;

import org.bukkit.command.CommandSender;
import ru.loper.suncore.api.command.SubCommand;

import java.util.Collections;
import java.util.List;

public class StartSubCommand implements SubCommand {
    @Override
    public void onCommand(CommandSender commandSender, String[] args) {

    }

    @Override
    public List<String> onTabCompleter(CommandSender commandSender, String[] args) {
        return Collections.emptyList();
    }
}
