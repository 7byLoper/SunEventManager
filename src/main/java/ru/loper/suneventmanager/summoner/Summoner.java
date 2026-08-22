package ru.loper.suneventmanager.summoner;

import java.util.Optional;
import lombok.Getter;
import ru.loper.suneventmanager.SunEventManager;
import ru.loper.suneventmanager.api.events.SummonerTimerSetEvent;
import ru.loper.suneventmanager.api.models.Event;
import ru.loper.suneventmanager.api.runnable.SummonerRunnable;
import ru.loper.suneventmanager.config.PluginConfigManager;
import ru.loper.suneventmanager.manager.VoteManager;

public class Summoner {
    private final PluginConfigManager configManager;
    private final SunEventManager plugin;

    @Getter
    private final VoteManager voteManager;

    private int voteTimer;
    private int eventTimer;

    public Summoner(PluginConfigManager configManager, SunEventManager plugin) {
        this.configManager = configManager;
        this.plugin = plugin;

        this.voteManager = new VoteManager(plugin);

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
            setVoteTimer(configManager.getVoteStartTime());
        } else {
            voteTimer = -1;
        }
    }

    public void resetEventTimer() {
        if (isEventTimer()) {
            setEventTimer(configManager.getEventsTime());
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

    public void setEventTimer(int seconds) {
        SummonerTimerSetEvent event = new SummonerTimerSetEvent(SummonerTimerSetEvent.TimerType.EVENT, seconds);
        event.callEvent();
        this.eventTimer = Math.max(0, event.getTimerValue());
    }

    public boolean isVoteTimer() {
        return configManager.isVoteEnabled();
    }

    public void setVoteTimer(int seconds) {
        SummonerTimerSetEvent event = new SummonerTimerSetEvent(SummonerTimerSetEvent.TimerType.VOTE, seconds);
        event.callEvent();
        this.voteTimer = Math.max(0, event.getTimerValue());
    }

    public void startRandomEvent() {
        Optional<Class<? extends Event>> optionalEvent =
                plugin.getEventManager().getRandomRegisteredEventChances();
        if (optionalEvent.isEmpty()) {
            plugin.getLogger().severe("Ошибка запуска случайного ивента: не найден ивент для запуска.");
            return;
        }

        try {
            Event event = optionalEvent.get().getDeclaredConstructor().newInstance();
            event.setRandomRarity();
            event.activate();
        } catch (Exception e) {
            plugin.getLogger()
                    .log(
                            java.util.logging.Level.SEVERE,
                            "Ошибка при создании ивента " + optionalEvent.get().getName(),
                            e);
        }
    }

    public String getEventStartTimeMessage() {
        int seconds = Math.max(eventTimer, voteTimer);
        return plugin.getConfigManager().getFormattedTimeMessage(seconds);
    }

    public int getCurrentVoteTimer() {
        return voteTimer;
    }

    public int getCurrentEventTimer() {
        return eventTimer;
    }
}
