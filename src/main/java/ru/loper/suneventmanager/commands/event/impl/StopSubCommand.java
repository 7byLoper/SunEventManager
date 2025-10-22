package ru.loper.suneventmanager.commands.event.impl;

import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;
import ru.loper.suncore.api.command.SubCommand;
import ru.loper.suncore.utils.Colorize;
import ru.loper.suneventmanager.api.modules.event.Event;
import ru.loper.suneventmanager.api.modules.event.EventManager;

import java.util.*;

@RequiredArgsConstructor
public class StopSubCommand implements SubCommand {
    private final EventManager eventManager;

    @Override
    public void onCommand(CommandSender sender, String[] args) {
        if (args.length == 1) {
            sender.sendMessage(Colorize.parse("&#FFF78F[EventManager] &fИспользование: &7/event stop <all | eventId | uuid>"));
            return;
        }

        String target = args[1];

        if (target.equalsIgnoreCase("all")) {
            handleStopAll(sender);
            return;
        }

        try {
            UUID uuid = UUID.fromString(target);
            handleStopByUuid(sender, uuid.toString());
            return;
        } catch (IllegalArgumentException ignored) {

        }

        if (args.length >= 3) {
            try {
                handleStopByEventIdAndUuid(sender, target, UUID.fromString(args[2]).toString());
                return;
            } catch (IllegalArgumentException ignored) {

            }
        }

        handleStopByEventId(sender, target);
    }

    private void handleStopAll(CommandSender sender) {
        int count = eventManager.getActiveEvents().size();
        eventManager.stopAllActiveEvents();
        sender.sendMessage(Colorize.parse(String.format(
                "&#FFF78F[EventManager] &fОстановлены все активные ивенты &7(&#ffff00%d&7",
                count
        )));
    }

    private void handleStopByUuid(CommandSender sender, String uuid) {
        Event event = eventManager.getEventByUUID(uuid);
        if (event == null) {
            sender.sendMessage(Colorize.parse("&#FFF78F[EventManager] &fИвент с UUID &#FF0000" + uuid.substring(0, 8) + "...&f не найден"));
            return;
        }

        event.updateStopping();
        sender.sendMessage(Colorize.parse(String.format(
                "&#FFF78F[EventManager] &fИвент &#00ff00%s&f &7(&fUUID: &#00ff00%s...&7)&f остановлен",
                event.getId(),
                uuid.substring(0, 8)
        )));
    }

    private void handleStopByEventIdAndUuid(CommandSender sender, String eventId, String uuid) {
        Event event = eventManager.getEventByUUID(uuid);
        if (event != null && event.getId().equalsIgnoreCase(eventId)) {
            event.updateStopping();
            sender.sendMessage(Colorize.parse(String.format(
                    "&#FFF78F[EventManager] &fИвент &#00ff00%s&f &7(&fUUID: &#00ff00%s...&7)&f остановлен",
                    eventId,
                    uuid.substring(0, 8)
            )));
        } else {
            handleStopByEventId(sender, eventId);
        }
    }

    private void handleStopByEventId(CommandSender sender, String eventId) {
        Set<Event> events = eventManager.getActiveEventsById(eventId);

        if (events.isEmpty()) {
            sender.sendMessage(Colorize.parse(String.format(
                    "&#FFF78F[EventManager] &fНе найдено активных ивентов для &#ff0000%s",
                    eventId
            )));
            return;
        }

        events.forEach(Event::updateStopping);
        sender.sendMessage(Colorize.parse(String.format(
                "&#FFF78F[EventManager] &fОстановлено &#00ff00%d&f ивентов с ID &#00ff00%s",
                events.size(),
                eventId
        )));
    }

    @Override
    public List<String> onTabCompleter(CommandSender commandSender, String[] args) {
        if (args.length == 2) {
            List<String> options = new ArrayList<>();
            options.add("all");

            eventManager.getActiveEvents().stream()
                    .map(Event::getId)
                    .distinct()
                    .forEach(options::add);

            eventManager.getActiveEvents().stream()
                    .map(event -> event.getUuid().toString())
                    .forEach(options::add);

            return options.stream()
                    .filter(option -> option.toLowerCase().startsWith(args[1].toLowerCase()))
                    .sorted()
                    .toList();
        }

        if (args.length == 3 && !args[1].equalsIgnoreCase("all")) {
            return eventManager.getActiveEvents().stream()
                    .filter(event -> event.getId().equalsIgnoreCase(args[1]))
                    .map(event -> event.getUuid().toString())
                    .filter(uuid -> uuid.toLowerCase().startsWith(args[2].toLowerCase()))
                    .sorted()
                    .toList();
        }

        return Collections.emptyList();
    }
}