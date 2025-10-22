package ru.loper.suneventmanager.api.modules.event;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import ru.loper.suneventmanager.SunEventManager;
import ru.loper.suneventmanager.api.modules.event.runnable.EventsRunnable;
import ru.loper.suneventmanager.config.EventConfigManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class EventManager {
    private final SunEventManager plugin;
    private final Map<String, EventConfigManager> configByEventId = new ConcurrentHashMap<>();
    private final Map<Plugin, Map<String, Class<? extends Event>>> registeredEvents = new ConcurrentHashMap<>();
    private final Set<Event> activeEvents = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public EventManager(SunEventManager plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(new InternalListener(), plugin);

        EventsRunnable eventsRunnable = new EventsRunnable(this);
        eventsRunnable.runTaskTimer(plugin, 20L, 20L);
    }

    public void registerEvent(Plugin plugin, String id, Class<? extends Event> eventClass, EventConfigManager configManager) {
        if (plugin == null || id == null || eventClass == null || configManager == null) {
            throw new IllegalArgumentException("Plugin, id, eventClass, and configManager must not be null");
        }

        configByEventId.put(id, configManager);

        registeredEvents.computeIfAbsent(plugin, k -> new ConcurrentHashMap<>()).put(id, eventClass);
        this.plugin.getLogger().info("Event '" + id + "' registered by " + plugin.getName());
    }

    public void unregisterEvents(Plugin plugin) {
        Map<String, Class<? extends Event>> pluginEvents = registeredEvents.remove(plugin);
        if (pluginEvents == null) return;

        for (String id : pluginEvents.keySet()) {
            getActiveEventsById(id).forEach(Event::updateStopping);
        }

        this.plugin.getLogger().info("All events from " + plugin.getName() + " unregistered");
    }

    public Map<String, Class<? extends Event>> getAllRegisteredEvents() {
        return registeredEvents.values().stream()
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (first, second) -> first
                ));
    }

    public Optional<Class<? extends Event>> getEventById(String id) {
        for (Map<String, Class<? extends Event>> pluginEvents : registeredEvents.values()) {
            for (Map.Entry<String, Class<? extends Event>> entry : pluginEvents.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(id)) {
                    return Optional.of(entry.getValue());
                }
            }
        }

        return Optional.empty();
    }

    public Event getEventByUUID(String uuid) {
        return activeEvents.stream()
                .filter(event -> event.getUuid().toString().equalsIgnoreCase(uuid))
                .findFirst()
                .orElse(null);
    }

    public Optional<Class<? extends Event>> getRandomRegisteredEvent() {
        List<Class<? extends Event>> allEvents = getAllRegisteredEvents().values().stream().toList();
        if (allEvents.isEmpty()) return Optional.empty();

        int index = new Random().nextInt(allEvents.size());
        return Optional.of(allEvents.get(index));
    }

    public Set<Event> getActiveEvents() {
        return Collections.unmodifiableSet(activeEvents);
    }

    public Map<String, Class<? extends Event>> getEventsByPlugin(Plugin plugin) {
        return Collections.unmodifiableMap(registeredEvents.getOrDefault(plugin, Collections.emptyMap()));
    }

    public void registerActiveEvent(Event event) {
        activeEvents.add(event);
    }

    public void unregisterActiveEvent(Event event) {
        activeEvents.remove(event);
    }

    public Set<Event> getActiveEventsById(String id) {
        return activeEvents.stream()
                .filter(event -> event.getId().equalsIgnoreCase(id))
                .collect(Collectors.toSet());
    }

    public EventConfigManager getEventConfigById(String id) {
        return configByEventId.get(id);
    }

    public void stopAllActiveEvents() {
        activeEvents.forEach(Event::updateStopping);
        activeEvents.clear();
    }

    public void clearAllRegisteredEvents() {
        registeredEvents.clear();
    }

    private class InternalListener implements Listener {
        @EventHandler
        public void onPluginDisable(PluginDisableEvent event) {
            unregisterEvents(event.getPlugin());
        }
    }
}
