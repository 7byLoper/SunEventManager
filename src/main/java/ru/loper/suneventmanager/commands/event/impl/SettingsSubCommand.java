package ru.loper.suneventmanager.commands.event.impl;

import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.loper.suncore.api.command.SubCommand;
import ru.loper.suneventmanager.config.PluginConfigManager;
import ru.loper.suneventmanager.menu.setiings.EventsMenu;

import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
public class SettingsSubCommand implements SubCommand {
    private final PluginConfigManager configManager;

    @Override
    public void onCommand(CommandSender commandSender, String[] args) {
        if (!(commandSender instanceof Player player)) {
            commandSender.sendMessage("Данная команда доступна только игрокам");
            return;
        }

        new EventsMenu(configManager).show(player);
    }

    @Override
    public List<String> onTabCompleter(CommandSender commandSender, String[] args) {
        return Collections.emptyList();
    }
}
