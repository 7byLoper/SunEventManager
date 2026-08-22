package ru.loper.suneventmanager.listeners;

import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import ru.loper.suneventmanager.manager.EventManager;

@RequiredArgsConstructor
public class InternalListener implements Listener {
    private final EventManager eventManager;

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        eventManager.unregisterEvents(event.getPlugin());
    }
}
