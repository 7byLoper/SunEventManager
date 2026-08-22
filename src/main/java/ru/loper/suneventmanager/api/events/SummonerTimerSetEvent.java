package ru.loper.suneventmanager.api.events;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class SummonerTimerSetEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final TimerType timerType;

    @Setter
    private int timerValue;

    public SummonerTimerSetEvent(TimerType timerType, int timerValue) {
        this.timerType = timerType;
        this.timerValue = timerValue;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public enum TimerType {
        VOTE,
        EVENT
    }
}
