package ru.loper.suneventmanager.message;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class PlaceholderApiResolver implements PlaceholderResolver {
    @Override
    public String resolve(CommandSender viewer, String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        OfflinePlayer player = viewer instanceof Player onlinePlayer ? onlinePlayer : null;
        return PlaceholderAPI.setPlaceholders(player, input);
    }
}
