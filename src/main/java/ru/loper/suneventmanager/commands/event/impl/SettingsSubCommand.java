package ru.loper.suneventmanager.commands.event.impl;

import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.loper.suncore.api.command.BuildableCommand;
import ru.loper.suncore.api.command.register.SubCommandRegister;
import ru.loper.suneventmanager.config.PluginConfigManager;
import ru.loper.suneventmanager.menu.setiings.EventsMenu;
import ru.loper.suneventmanager.message.ConfiguredMessages;

@RequiredArgsConstructor
@SubCommandRegister(permission = "suneventmanager.command.settings", aliases = "settings")
public class SettingsSubCommand implements BuildableCommand {
    private final PluginConfigManager configManager;

    @Override
    public void handle(@NotNull CommandSender commandSender, String[] args) {
        if (!(commandSender instanceof Player player)) {
            ConfiguredMessages.send(
                    commandSender, "commands.settings.only_player", "Данная команда доступна только игрокам");
            return;
        }

        new EventsMenu(configManager).show(player);
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender commandSender, String[] args) {
        return Collections.emptyList();
    }
}
