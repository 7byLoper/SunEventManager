package ru.loper.suneventmanager.commands.event.impl;

import java.util.*;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import ru.loper.suncore.api.command.BuildableCommand;
import ru.loper.suncore.api.command.register.SubCommandRegister;
import ru.loper.suneventmanager.api.models.Event;
import ru.loper.suneventmanager.manager.EventManager;
import ru.loper.suneventmanager.message.ConfiguredMessages;

@RequiredArgsConstructor
@SubCommandRegister(permission = "suneventmanager.command.settimer", aliases = "settimer")
public class SetTimerSubCommand implements BuildableCommand {
    private final EventManager eventManager;

    @Override
    public void handle(@NotNull CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendUsage(sender);
            return;
        }

        int seconds;
        try {
            seconds = Integer.parseInt(args[2]);
        } catch (NumberFormatException exception) {
            ConfiguredMessages.send(
                    sender,
                    "commands.settimer.invalid_number",
                    "&#FFF78F[EventManager] &fВремя должно быть целым числом секунд.");
            return;
        }

        if (seconds < 0) {
            ConfiguredMessages.send(
                    sender,
                    "commands.settimer.negative",
                    "&#FFF78F[EventManager] &fВремя не может быть отрицательным.");
            return;
        }

        Collection<Event> events = resolveEvents(args);
        if (events.isEmpty()) {
            ConfiguredMessages.send(
                    sender,
                    "commands.settimer.no_events",
                    "&#FFF78F[EventManager] &fПодходящие активные ивенты не найдены.");
            return;
        }

        events.forEach(event -> event.setEventTimer(seconds));
        ConfiguredMessages.send(
                sender,
                "commands.settimer.success",
                "&#FFF78F[EventManager] &fТаймер установлен на &#ffff00{seconds}&f сек. для ивентов: &#00ff00{count}",
                "seconds",
                seconds,
                "count",
                events.size());
    }

    private Collection<Event> resolveEvents(String[] args) {
        String target = args[1];

        if (target.equalsIgnoreCase("all")) {
            return eventManager.getActiveEvents().stream()
                    .filter(Event::isStarted)
                    .toList();
        }

        try {
            Event event = eventManager.getEventByUUID(UUID.fromString(target).toString());
            return event != null && event.isStarted() ? List.of(event) : Collections.emptyList();
        } catch (IllegalArgumentException ignored) {
            // The target is an event id, not a UUID.
        }

        if (args.length >= 4) {
            try {
                Event event =
                        eventManager.getEventByUUID(UUID.fromString(args[3]).toString());
                return event != null && event.isStarted() && event.getId().equalsIgnoreCase(target)
                        ? List.of(event)
                        : Collections.emptyList();
            } catch (IllegalArgumentException ignored) {
                return Collections.emptyList();
            }
        }

        return eventManager.getActiveEventsById(target).stream()
                .filter(Event::isStarted)
                .toList();
    }

    private void sendUsage(CommandSender sender) {
        ConfiguredMessages.send(
                sender,
                "commands.settimer.usage",
                "&#FFF78F[EventManager] &fИспользование: &7/event settimer \\<all | eventId | uuid> \\<seconds> [uuid]");
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, String[] args) {
        if (args.length == 2) {
            List<String> options = new ArrayList<>();
            options.add("all");
            eventManager.getActiveEvents().stream()
                    .filter(Event::isStarted)
                    .map(Event::getId)
                    .distinct()
                    .forEach(options::add);
            eventManager.getActiveEvents().stream()
                    .filter(Event::isStarted)
                    .map(event -> event.getUniqueId().toString())
                    .forEach(options::add);
            return options.stream()
                    .filter(option -> option.toLowerCase().startsWith(args[1].toLowerCase()))
                    .sorted()
                    .toList();
        }

        if (args.length == 3) {
            return Stream.of("60", "300", "600")
                    .filter(option -> option.startsWith(args[2]))
                    .toList();
        }

        if (args.length == 4 && !args[1].equalsIgnoreCase("all")) {
            return eventManager.getActiveEventsById(args[1]).stream()
                    .filter(Event::isStarted)
                    .map(event -> event.getUniqueId().toString())
                    .filter(uuid -> uuid.toLowerCase().startsWith(args[3].toLowerCase()))
                    .sorted()
                    .toList();
        }

        return Collections.emptyList();
    }
}
