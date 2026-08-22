package ru.loper.suneventmanager.commands.event.impl;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.loper.suncore.api.command.BuildableCommand;
import ru.loper.suncore.api.command.register.SubCommandRegister;
import ru.loper.suneventmanager.SunEventManager;
import ru.loper.suneventmanager.api.models.Event;
import ru.loper.suneventmanager.message.ConfiguredMessages;

@RequiredArgsConstructor
@SubCommandRegister(permission = "suneventmanager.command.gps", aliases = "gps")
public class GpsSubCommand implements BuildableCommand {
    private final SunEventManager plugin;

    @Override
    public void handle(@NotNull CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ConfiguredMessages.send(sender, "commands.gps.only_player", "<red>Эта команда доступна только игроку.");
            return;
        }

        if (!Bukkit.getPluginManager().isPluginEnabled("FunUtils")) {
            ConfiguredMessages.send(player, "commands.gps.funutils_unavailable", "<red>GPS временно недоступен.");
            return;
        }

        List<Event> events = plugin.getEventManager().getActiveEvents().stream()
                .filter(Event::isActivate)
                .filter(Event::isStarted)
                .filter(event -> event.getEventLocation() != null)
                .sorted(Comparator.comparing(Event::getId).thenComparing(Event::getUniqueId))
                .toList();

        if (events.isEmpty()) {
            ConfiguredMessages.send(
                    player,
                    "commands.gps.no_active_event",
                    "<red>Сейчас нет активного события с известными координатами.");
            return;
        }

        Optional<Event> target = findTarget(events, args);
        if (target.isEmpty()) {
            ConfiguredMessages.send(
                    player,
                    "commands.gps.event_not_found",
                    "<red>Активное событие <white>{event}<red> не найдено.",
                    "event",
                    args[1]);
            return;
        }

        Location location = target.get().getEventLocation();
        player.performCommand("gps " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ());
    }

    private Optional<Event> findTarget(List<Event> events, String[] args) {
        if (args.length < 2 || args[1].isBlank()) {
            return events.stream().findFirst();
        }

        String input = args[1];
        return events.stream()
                .filter(event -> event.getId().equalsIgnoreCase(input)
                        || event.getUniqueId().toString().equalsIgnoreCase(input))
                .findFirst();
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, String[] args) {
        if (args.length != 2) {
            return List.of();
        }

        String input = args[1].toLowerCase(java.util.Locale.ROOT);
        return plugin.getEventManager().getActiveEvents().stream()
                .filter(Event::isActivate)
                .filter(Event::isStarted)
                .filter(event -> event.getEventLocation() != null)
                .map(Event::getId)
                .distinct()
                .filter(id -> id.toLowerCase(java.util.Locale.ROOT).startsWith(input))
                .sorted()
                .toList();
    }
}
