package ru.loper.suneventmanager.commands.event.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import ru.loper.suncore.api.command.BuildableCommand;
import ru.loper.suncore.api.command.register.SubCommandRegister;
import ru.loper.suneventmanager.manager.SummonBlockManager;
import ru.loper.suneventmanager.message.ConfiguredMessages;

@RequiredArgsConstructor
@SubCommandRegister(permission = "suneventmanager.command.summon", aliases = "summon")
public final class SummonSubCommand implements BuildableCommand {
    private final SummonBlockManager summonBlockManager;

    @Override
    public void handle(@NotNull CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendUsage(sender);
            return;
        }

        if (args[1].equalsIgnoreCase("list")) {
            sendList(sender);
            return;
        }

        if (!args[1].equalsIgnoreCase("give")) {
            sendUsage(sender);
            return;
        }

        give(sender, args);
    }

    private void give(CommandSender sender, String[] args) {
        if (!sender.hasPermission("suneventmanager.command.summon.give")) {
            ConfiguredMessages.send(sender, "commands.summon.no_permission", "&#FF2222▶&f У вас недостаточно прав.");
            return;
        }
        if (args.length < 4) {
            sendUsage(sender);
            return;
        }

        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            ConfiguredMessages.send(sender, "commands.summon.player_not_found", "&#FF2222▶&f Игрок не найден.");
            return;
        }

        int amount = parseAmount(sender, args);
        if (amount < 1) {
            return;
        }

        ItemStack item = summonBlockManager.createItem(args[3], amount);
        if (item == null) {
            ConfiguredMessages.send(
                    sender,
                    "commands.summon.unknown_id",
                    "&#FF2222▶&f Неизвестный summon id: &#FF2222{id}",
                    "id",
                    args[3]);
            return;
        }

        target.getInventory()
                .addItem(item)
                .values()
                .forEach(leftover -> target.getWorld().dropItemNaturally(target.getLocation(), leftover));
        ConfiguredMessages.send(
                sender,
                "commands.summon.given",
                "&#22FF88▶&f Выдан summon-item &#22FF88{id}&f игроку &#22FF88{player}&f.",
                "id",
                args[3],
                "player",
                target.getName(),
                "amount",
                amount);
    }

    private int parseAmount(CommandSender sender, String[] args) {
        if (args.length < 5) {
            return 1;
        }

        try {
            return Math.max(1, Math.min(64, Integer.parseInt(args[4])));
        } catch (NumberFormatException ignored) {
            ConfiguredMessages.send(
                    sender, "commands.summon.invalid_amount", "&#FF2222▶&f Количество должно быть числом от 1 до 64.");
            return -1;
        }
    }

    private void sendList(CommandSender sender) {
        if (summonBlockManager.getIds().isEmpty()) {
            ConfiguredMessages.send(
                    sender, "commands.summon.empty", "&#FFF78F[EventManager] &fSummon-item'ы не настроены.");
            return;
        }

        ConfiguredMessages.send(
                sender,
                "commands.summon.list",
                "&#FFF78F[EventManager] &fSummon-item'ы: &7{ids}",
                "ids",
                String.join(", ", summonBlockManager.getIds()));
    }

    private void sendUsage(CommandSender sender) {
        ConfiguredMessages.send(
                sender,
                "commands.summon.usage",
                "&#FFF78F[EventManager] &fИспользование: &7/event summon \\<list | give \\<player> \\<id> [amount]>");
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, String[] args) {
        if (args.length == 2) {
            return filter(List.of("list", "give"), args[1]);
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("give")) {
            return filter(
                    Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[2]);
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("give")) {
            return filter(new ArrayList<>(summonBlockManager.getIds()), args[3]);
        }
        return List.of();
    }

    private List<String> filter(List<String> values, String input) {
        String prefix = input == null ? "" : input.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix))
                .sorted()
                .toList();
    }
}
