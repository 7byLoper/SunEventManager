package ru.loper.suneventmanager.commands.event.impl;

import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.loper.suncore.api.command.BuildableCommand;
import ru.loper.suncore.api.command.register.SubCommandRegister;
import ru.loper.suneventmanager.api.models.Event;
import ru.loper.suneventmanager.api.models.EventRarity;
import ru.loper.suneventmanager.manager.EventManager;
import ru.loper.suneventmanager.message.ConfiguredMessages;

@RequiredArgsConstructor
@SubCommandRegister(permission = "suneventmanager.command.start", aliases = "start")
public class StartSubCommand implements BuildableCommand {
    private final EventManager eventManager;

    @Override
    public void handle(@NotNull CommandSender sender, String[] args) {
        if (args.length == 1) {
            ConfiguredMessages.send(
                    sender,
                    "commands.start.usage",
                    "&#FFF78F[EventManager] &fИспользование: &7/event start \\<id> [rarity] [--this]");
            return;
        }

        String eventId = args[1];
        Optional<EventManager.WrappedEvent> wrapped = eventManager.getEventById(eventId);
        if (wrapped.isEmpty()) {
            ConfiguredMessages.send(
                    sender,
                    "commands.start.event_not_found",
                    "&#FFF78F[EventManager] &fИвент с ID &#ffff00{event}&f не найден",
                    "event",
                    eventId);
            return;
        }

        try {
            Event event = wrapped.get().eventClass().getDeclaredConstructor().newInstance();
            if (!applyRarity(event, args, sender)) {
                return;
            }

            applyLocation(event, sender, args);
            event.activate();
            if (!event.isActivate()) {
                ConfiguredMessages.send(
                        sender,
                        "commands.start.start_failed",
                        "&#FFF78F[EventManager] &fИвент &#ffff00{event}&f не удалось запустить.",
                        "event",
                        eventId);
                return;
            }

            ConfiguredMessages.send(
                    sender,
                    "commands.start.started",
                    "&#FFF78F[EventManager] &fИвент &#ffff00{event}&f запущен{rarity_suffix}",
                    "event",
                    eventId,
                    "rarity",
                    event.getEventRarity() == null ? "" : event.getEventRarity().getName(),
                    "rarity_suffix",
                    getRaritySuffix(event));
        } catch (ReflectiveOperationException | IllegalStateException e) {
            ConfiguredMessages.send(
                    sender,
                    "commands.start.create_error",
                    "&#FFF78F[EventManager] &fОшибка при создании ивента: &#FF0000{error}",
                    "error",
                    e.getMessage());
        }
    }

    private boolean applyRarity(Event event, String[] args, CommandSender sender) {
        if (args.length < 3 || args[2].startsWith("--") || args[2].equalsIgnoreCase("random")) {
            event.setRandomRarity();
            return true;
        }

        Optional<EventRarity> rarity =
                event.getConfigManager().getRarityManager().getRarity(args[2]);
        if (rarity.isEmpty()) {
            ConfiguredMessages.send(
                    sender,
                    "commands.start.rarity_not_found",
                    "&#FFF78F[EventManager] &fРедкость &#FF0000{rarity}&f не найдена для ивента &#ffff00{event}",
                    "rarity",
                    args[2],
                    "event",
                    event.getId());
            return false;
        }

        event.setEventRarity(rarity.get());
        return true;
    }

    private void applyLocation(Event event, CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            return;
        }

        boolean useCurrentLocation = Arrays.stream(args).anyMatch(arg -> arg.equalsIgnoreCase("--this"));
        if (!useCurrentLocation) {
            return;
        }

        Location location = player.getLocation().clone().toBlockLocation();
        location.setY(location.getWorld().getHighestBlockYAt(location) + 1);
        event.setEventLocation(location);
    }

    private String getRaritySuffix(Event event) {
        return event.getEventRarity() == null
                ? ""
                : " с редкостью &#00AAFF" + event.getEventRarity().getName();
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, String[] args) {
        if (args.length == 2) {
            String input = args[1].toLowerCase(Locale.ROOT);
            return eventManager.getAllRegisteredEvents().keySet().stream()
                    .filter(id -> id.toLowerCase(Locale.ROOT).startsWith(input))
                    .sorted()
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && !args[2].startsWith("--")) {
            var wrappedEvent = eventManager.getEventById(args[1]);
            if (wrappedEvent.isPresent()) {
                String input = args[2].toLowerCase(Locale.ROOT);
                List<String> rarities =
                        wrappedEvent.get().configManager().getRarityManager().getRarities().values().stream()
                                .map(EventRarity::getName)
                                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(input))
                                .sorted()
                                .collect(Collectors.toList());
                if ("random".startsWith(input)) {
                    rarities.add(0, "random");
                }
                return rarities;
            }
        }

        if (args.length >= 3 && args[args.length - 1].isEmpty()) {
            return Collections.singletonList("--this");
        }
        return Collections.emptyList();
    }
}
