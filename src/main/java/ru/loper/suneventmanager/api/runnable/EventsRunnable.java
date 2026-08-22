package ru.loper.suneventmanager.api.runnable;

import java.util.logging.Level;
import lombok.RequiredArgsConstructor;
import ru.loper.suncore.api.scheduler.CoreRunnable;
import ru.loper.suneventmanager.SunEventManager;
import ru.loper.suneventmanager.api.models.Event;
import ru.loper.suneventmanager.manager.EventManager;

@RequiredArgsConstructor
public class EventsRunnable extends CoreRunnable {
    private final EventManager eventManager;

    @Override
    public void run() {
        for (Event activeEvent : eventManager.getActiveEvents()) {
            if (!activeEvent.isActivate()) {
                continue;
            }

            try {
                activeEvent.eventTimerProgress();
            } catch (Exception exception) {
                SunEventManager.getInstance()
                        .getLogger()
                        .log(
                                Level.SEVERE,
                                "Ошибка обновления ивента '" + activeEvent.getId() + "' (" + activeEvent.getUniqueId()
                                        + ")",
                                exception);

                try {
                    activeEvent.updateStopping();
                } catch (Exception cleanupException) {
                    SunEventManager.getInstance()
                            .getLogger()
                            .log(
                                    Level.SEVERE,
                                    "Ошибка аварийной остановки ивента '" + activeEvent.getId() + "'",
                                    cleanupException);
                    eventManager.unregisterActiveEvent(activeEvent);
                }
            }
        }
    }
}
