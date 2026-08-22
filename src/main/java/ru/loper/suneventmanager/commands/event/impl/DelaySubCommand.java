package ru.loper.suneventmanager.commands.event.impl;

import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import ru.loper.suncore.api.command.BuildableCommand;
import ru.loper.suncore.api.command.register.SubCommandRegister;
import ru.loper.suneventmanager.SunEventManager;
import ru.loper.suneventmanager.api.models.Event;
import ru.loper.suneventmanager.config.PluginConfigManager;
import ru.loper.suneventmanager.utils.TimeUtils;

@RequiredArgsConstructor
@SubCommandRegister(permission = "suneventmanager.command.delay", aliases = "delay")
public class DelaySubCommand implements BuildableCommand {
    private final SunEventManager plugin;

    @Override
    public void handle(@NotNull CommandSender sender, String[] args) {
        PluginConfigManager config = plugin.getConfigManager();
        boolean sent = false;

        List<Event> activeEvents = plugin.getEventManager().getActiveEvents().stream()
                .filter(Event::isActivate)
                .filter(Event::isStarted)
                .sorted(Comparator.comparing(Event::getId).thenComparing(Event::getUniqueId))
                .toList();

        int index = 1;
        for (Event event : activeEvents) {
            int currentIndex = index++;
            String message = event.getTimerMessage(currentIndex);
            if (!message.isEmpty()) {
                java.util.Map<String, String> placeholders =
                        new java.util.LinkedHashMap<>(event.getMessagePlaceholders());
                placeholders.put("index", String.valueOf(currentIndex));
                placeholders.put("i", String.valueOf(currentIndex));
                plugin.getMessageService().send(sender, message, placeholders);
                sent = true;
            }
        }

        if (plugin.getSummoner().getVoteManager().isVoteInProgress()) {
            int seconds = Math.max(
                    0, plugin.getSummoner().getVoteManager().getCurrentVote().getVoteTimer());
            plugin.getMessageService()
                    .send(
                            sender,
                            config.getEventDelayVoteMessage(),
                            plugin.getMessageService().placeholders("time", TimeUtils.formatSeconds(seconds)));
            sent = true;
        }

        if (plugin.getSummoner().isEventTimer()) {
            plugin.getMessageService()
                    .send(
                            sender,
                            config.getEventDelayNextEventMessage(),
                            plugin.getMessageService()
                                    .placeholders(
                                            "time",
                                            TimeUtils.formatSeconds(Math.max(
                                                    0, plugin.getSummoner().getCurrentEventTimer()))));
            sent = true;
        }

        if (plugin.getSummoner().isVoteTimer()) {
            plugin.getMessageService()
                    .send(
                            sender,
                            config.getEventDelayNextVoteMessage(),
                            plugin.getMessageService()
                                    .placeholders(
                                            "time",
                                            TimeUtils.formatSeconds(Math.max(
                                                    0, plugin.getSummoner().getCurrentVoteTimer()))));
            sent = true;
        }

        if (!sent && !config.getEventDelayNothingMessage().isEmpty()) {
            plugin.getMessageService()
                    .send(sender, config.getEventDelayNothingMessage(), java.util.Collections.emptyMap());
        }
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, String[] args) {
        return List.of();
    }
}
