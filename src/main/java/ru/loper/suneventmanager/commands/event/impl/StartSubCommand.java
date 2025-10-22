package ru.loper.suneventmanager.commands.event.impl;

import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.loper.suncore.api.command.SubCommand;
import ru.loper.suncore.utils.Colorize;
import ru.loper.suneventmanager.api.modules.event.Event;
import ru.loper.suneventmanager.api.modules.event.EventManager;
import ru.loper.suneventmanager.api.modules.event.rarity.EventRarity;
import ru.loper.suneventmanager.config.EventConfigManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class StartSubCommand implements SubCommand {
    private final EventManager eventManager;

    @Override
    public void onCommand(CommandSender sender, String[] args) {
        if (args.length == 1) {
            sender.sendMessage(Colorize.parse("&#FFF78F[EventManager] &fИспользование: &7/event start <id> [rarity] [--this]"));
            return;
        }

        String eventId = args[1];
        Optional<Class<? extends Event>> eventClassOpt = eventManager.getEventById(eventId);

        if (eventClassOpt.isEmpty()) {
            sender.sendMessage(Colorize.parse(String.format(
                    "&#FFF78F[EventManager] &fИвент с ID &#ffff00%s&f не найден",
                    eventId
            )));
            return;
        }

        try {
            Event event = eventClassOpt.get()
                    .getConstructor()
                    .newInstance();

            handleRarityArgument(event, args);
            handleLocationArgument(event, sender, args);

            event.activate();

            sender.sendMessage(Colorize.parse(String.format(
                    "&#FFF78F[EventManager] &fИвент &#ffff00%s&f запущен%s",
                    eventId,
                    getRaritySuffix(event)
            )));
        } catch (Exception e) {
            sender.sendMessage(Colorize.parse("&#FFF78F[EventManager] &fОшибка при создании ивента: &#FF0000" + e.getMessage()));
        }
    }

    private void handleRarityArgument(Event event, String[] args) {
        if (args.length < 3) return;

        String rarityArg = args[1];
        if (rarityArg.startsWith("--")) return;

        event.getConfigManager().getRarityManager().getRarity(rarityArg).ifPresent(event::setEventRarity);
    }

    private void handleLocationArgument(Event event, CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) return;

        boolean useCurrentLocation = Arrays.stream(args).anyMatch(arg -> arg.equalsIgnoreCase("--this"));

        if (useCurrentLocation) {
            Player player = (Player) sender;
            Location location = player.getLocation().clone().toBlockLocation();
            location.setY(location.getWorld().getHighestBlockYAt(location) + 1);
            event.setEventLocation(location);
        }
    }

    private String getRaritySuffix(Event event) {
        return event.getEventRarity() != null ?
                String.format(" с редкостью &#00AAFF%s", event.getEventRarity().getName()) :
                "";
    }

    @Override
    public List<String> onTabCompleter(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return eventManager.getAllRegisteredEvents().keySet().stream()
                    .filter(id -> id.toLowerCase().startsWith(args[1].toLowerCase()))
                    .sorted()
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && !args[2].startsWith("--")) {
            EventConfigManager config = eventManager.getEventConfigById(args[1]);
            if (config != null) {
                return config.getRarityManager().getRarities().values()
                        .stream()
                        .map(EventRarity::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                        .sorted()
                        .collect(Collectors.toList());
            }
        }

        if (args.length >= 3 && args[args.length - 1].isEmpty()) {
            return Collections.singletonList("--this");
        }

        return Collections.emptyList();
    }
}