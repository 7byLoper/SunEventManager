package ru.loper.suneventmanager.api.modules.event.runnable;

import lombok.RequiredArgsConstructor;
import org.bukkit.scheduler.BukkitRunnable;
import ru.loper.suneventmanager.api.modules.event.Event;
import ru.loper.suneventmanager.api.modules.event.EventManager;

@RequiredArgsConstructor
public class EventsRunnable extends BukkitRunnable {
    private final EventManager eventManager;

    @Override
    public void run() {
        for (Event activeEvent : eventManager.getActiveEvents()) {
            if (!activeEvent.isActivate()) continue;
            activeEvent.eventTimerProgress();
        }


    }
}
