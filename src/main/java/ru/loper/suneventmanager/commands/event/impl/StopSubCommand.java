package ru.loper.suneventmanager.commands.event.impl;

import java.util.*;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import ru.loper.suncore.api.command.BuildableCommand;
import ru.loper.suncore.api.command.register.SubCommandRegister;
import ru.loper.suneventmanager.api.models.Event;
import ru.loper.suneventmanager.manager.EventManager;
import ru.loper.suneventmanager.message.ConfiguredMessages;

@RequiredArgsConstructor
@SubCommandRegister(permission = "suneventmanager.command.stop", aliases = "stop")
public class StopSubCommand implements BuildableCommand {
    private final EventManager eventManager;

    @Override
    public void handle(@NotNull CommandSender sender, String[] args) {
        if (args.length == 1) {
            ConfiguredMessages.send(
                    sender,
                    "commands.stop.usage",
                    "&#FFF78F[EventManager] &fИспользование: &7/event stop \\<all | eventId | uuid>");
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
                handleStopByEventIdAndUuid(
                        sender, target, UUID.fromString(args[2]).toString());
                return;
            } catch (IllegalArgumentException ignored) {

            }
        }

        handleStopByEventId(sender, target);
    }

    private void handleStopAll(CommandSender sender) {
        int count = eventManager.getActiveEvents().size();
        eventManager.stopAllActiveEvents();
        ConfiguredMessages.send(
                sender,
                "commands.stop.all",
                "&#FFF78F[EventManager] &fОстановлены все активные ивенты &7(&#ffff00{count}&7)",
                "count",
                count);
    }

    private void handleStopByUuid(CommandSender sender, String uuid) {
        Event event = eventManager.getEventByUUID(uuid);
        if (event == null) {
            ConfiguredMessages.send(
                    sender,
                    "commands.stop.uuid_not_found",
                    "&#FFF78F[EventManager] &fИвент с UUID &#FF0000{uuid}...&f не найден",
                    "uuid",
                    uuid.substring(0, 8));
            return;
        }

        event.updateStopping();
        ConfiguredMessages.send(
                sender,
                "commands.stop.one",
                "&#FFF78F[EventManager] &fИвент &#00ff00{event}&f &7(&fUUID: &#00ff00{uuid}...&7)&f остановлен",
                "event",
                event.getId(),
                "uuid",
                uuid.substring(0, 8));
    }

    private void handleStopByEventIdAndUuid(CommandSender sender, String eventId, String uuid) {
        Event event = eventManager.getEventByUUID(uuid);
        if (event != null && event.getId().equalsIgnoreCase(eventId)) {
            event.updateStopping();
            ConfiguredMessages.send(
                    sender,
                    "commands.stop.one",
                    "&#FFF78F[EventManager] &fИвент &#00ff00{event}&f &7(&fUUID: &#00ff00{uuid}...&7)&f остановлен",
                    "event",
                    eventId,
                    "uuid",
                    uuid.substring(0, 8));
        } else {
            handleStopByEventId(sender, eventId);
        }
    }

    private void handleStopByEventId(CommandSender sender, String eventId) {
        Set<Event> events = eventManager.getActiveEventsById(eventId);

        if (events.isEmpty()) {
            ConfiguredMessages.send(
                    sender,
                    "commands.stop.event_not_found",
                    "&#FFF78F[EventManager] &fНе найдено активных ивентов для &#ff0000{event}",
                    "event",
                    eventId);
            return;
        }

        events.forEach(Event::updateStopping);
        ConfiguredMessages.send(
                sender,
                "commands.stop.event_count",
                "&#FFF78F[EventManager] &fОстановлено &#00ff00{count}&f ивентов с ID &#00ff00{event}",
                "count",
                events.size(),
                "event",
                eventId);
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender commandSender, String[] args) {
        if (args.length == 2) {
            List<String> options = new ArrayList<>();
            options.add("all");

            eventManager.getActiveEvents().stream().map(Event::getId).distinct().forEach(options::add);

            eventManager.getActiveEvents().stream()
                    .map(event -> event.getUniqueId().toString())
                    .forEach(options::add);

            return options.stream()
                    .filter(option -> option.toLowerCase().startsWith(args[1].toLowerCase()))
                    .sorted()
                    .toList();
        }

        if (args.length == 3 && !args[1].equalsIgnoreCase("all")) {
            return eventManager.getActiveEvents().stream()
                    .filter(event -> event.getId().equalsIgnoreCase(args[1]))
                    .map(event -> event.getUniqueId().toString())
                    .filter(uuid -> uuid.toLowerCase().startsWith(args[2].toLowerCase()))
                    .sorted()
                    .toList();
        }

        return Collections.emptyList();
    }
}
