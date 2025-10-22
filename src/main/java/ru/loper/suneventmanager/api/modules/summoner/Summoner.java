package ru.loper.suneventmanager.api.modules.summoner;

import lombok.Getter;
import ru.loper.suneventmanager.SunEventManager;
import ru.loper.suneventmanager.api.modules.event.Event;
import ru.loper.suneventmanager.api.modules.summoner.runnable.SummonerRunnable;
import ru.loper.suneventmanager.api.modules.vote.VoteManager;
import ru.loper.suneventmanager.config.PluginConfigManager;

import java.util.Optional;

public class Summoner {
    private final PluginConfigManager configManager;
    private final SunEventManager plugin;

    @Getter
    private final VoteManager voteManager;

    private int voteTimer;
    private int eventTimer;

    public Summoner(PluginConfigManager configManager, SunEventManager plugin) {
        this.voteManager = new VoteManager(plugin);
        this.configManager = configManager;
        this.plugin = plugin;

        SummonerRunnable summonerRunnable = new SummonerRunnable(this);
        summonerRunnable.runTaskTimer(plugin, 20L, 20L);

        resetTimers();
    }

    public void resetTimers() {
        resetEventTimer();
        resetVoteTimer();
    }

    public void resetVoteTimer() {
        if (isVoteTimer()) {
            voteTimer = configManager.getVoteStartTime();
        } else {
            voteTimer = -1;
        }
    }

    public void resetEventTimer() {
        if (isEventTimer()) {
            eventTimer = configManager.getEventsTime();
        } else {
            eventTimer = -1;
        }
    }

    public int getAndSubtractEventTimer() {
        return eventTimer--;
    }

    public int getAndSubtractVoteTimer() {
        return voteTimer--;
    }

    public boolean isEventTimer() {
        return configManager.isEventsEnabled();
    }

    public boolean isVoteTimer() {
        return configManager.isVoteEnabled();
    }

    public void startRandomEvent() {
        Optional<Class<? extends Event>> optionalEvent = plugin.getEventManager().getRandomRegisteredEvent();
        if (optionalEvent.isEmpty()) {
            plugin.getLogger().severe("Ошибка запуска случайного ивента: нет зарегистрированных ивентов.");
            return;
        }

        try {
            Event event = optionalEvent.get().getDeclaredConstructor().newInstance();
            event.activate();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при создании ивента " + optionalEvent.get().getName(), e);
        }
    }

    public String getEventStartTimeMessage() {
        int seconds = Math.max(eventTimer, voteTimer);
        return plugin.getConfigManager().getFormattedTimeMessage(seconds);
    }
}
