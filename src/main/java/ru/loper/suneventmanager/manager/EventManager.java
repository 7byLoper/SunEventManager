package ru.loper.suneventmanager.manager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import ru.loper.suneventmanager.SunEventManager;
import ru.loper.suneventmanager.api.models.Event;
import ru.loper.suneventmanager.api.models.EventRarity;
import ru.loper.suneventmanager.api.runnable.EventsRunnable;
import ru.loper.suneventmanager.config.EventConfigManager;

public class EventManager {
    private final Plugin plugin;
    private final Map<Plugin, Map<String, WrappedEvent>> registeredEvents;
    private final Set<Event> activeEvents;

    public EventManager(Plugin plugin) {
        this.plugin = plugin;
        this.registeredEvents = new ConcurrentHashMap<>();
        this.activeEvents = Collections.newSetFromMap(new ConcurrentHashMap<>());

        new EventsRunnable(this).runTaskTimer(plugin, 20L, 20L);
    }

    public void registerEvent(
            Plugin owner, String id, Class<? extends Event> eventClass, EventConfigManager configManager) {
        if (owner == null || id == null || id.isBlank() || eventClass == null || configManager == null) {
            plugin.getLogger()
                    .severe("Event registration skipped: owner, id, event class or config manager is invalid.");
            return;
        }

        String normalizedId = normalizeId(id);
        boolean usedByAnotherPlugin = registeredEvents.entrySet().stream()
                .anyMatch(entry -> entry.getKey() != owner && entry.getValue().containsKey(normalizedId));
        if (usedByAnotherPlugin) {
            plugin.getLogger()
                    .severe("Event id '" + id + "' is already registered by another plugin. Registration skipped.");
            return;
        }

        WrappedEvent wrappedEvent = new WrappedEvent(eventClass, configManager);
        registeredEvents
                .computeIfAbsent(owner, ignored -> new ConcurrentHashMap<>())
                .put(normalizedId, wrappedEvent);

        SunEventManager managerPlugin = SunEventManager.getInstance();
        if (managerPlugin != null && managerPlugin.getSummonBlockManager() != null) {
            managerPlugin.getSummonBlockManager().registerEventConfig(owner, normalizedId, configManager);
        }

        plugin.getLogger().info("Event '" + id + "' registered by " + owner.getName());
    }

    public void unregisterEvents(Plugin owner) {
        Map<String, WrappedEvent> pluginEvents = registeredEvents.remove(owner);
        if (pluginEvents == null) {
            return;
        }

        Set<String> ids = pluginEvents.keySet();
        new ArrayList<>(activeEvents)
                .stream()
                        .filter(event -> ids.contains(normalizeId(event.getId())))
                        .forEach(Event::updateStopping);

        SunEventManager managerPlugin = SunEventManager.getInstance();
        if (managerPlugin != null && managerPlugin.getSummonBlockManager() != null) {
            managerPlugin.getSummonBlockManager().unregisterEventConfigs(owner);
        }

        plugin.getLogger().info("All events from " + owner.getName() + " unregistered");
    }

    public Map<String, WrappedEvent> getAllRegisteredEvents() {
        Map<String, WrappedEvent> result = new LinkedHashMap<>();
        registeredEvents.values().forEach(events -> events.forEach(result::putIfAbsent));
        return Collections.unmodifiableMap(result);
    }

    public Optional<WrappedEvent> getEventById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }

        String normalizedId = normalizeId(id);
        for (Map<String, WrappedEvent> pluginEvents : registeredEvents.values()) {
            WrappedEvent event = pluginEvents.get(normalizedId);
            if (event != null) {
                return Optional.of(event);
            }
        }

        return Optional.empty();
    }

    public Event getEventByUUID(String uuid) {
        if (uuid == null) {
            return null;
        }

        return activeEvents.stream()
                .filter(event -> event.getUniqueId().toString().equalsIgnoreCase(uuid))
                .findFirst()
                .orElse(null);
    }

    public Optional<WrappedEvent> getRandomRegisteredEvent() {
        List<WrappedEvent> allEvents = getAllRegisteredEvents().values().stream()
                .filter(event -> event.configManager().isAutomaticStartEnabled())
                .toList();
        if (allEvents.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(allEvents.get(ThreadLocalRandom.current().nextInt(allEvents.size())));
    }

    public double getChance(String eventId) {
        return getEventById(eventId)
                .map(wrappedEvent -> wrappedEvent.configManager().getChance())
                .orElse(0.0);
    }

    public Set<Event> getActiveEvents() {
        return Collections.unmodifiableSet(activeEvents);
    }

    public Optional<Class<? extends Event>> getRandomRegisteredEventChances() {
        Map<String, WrappedEvent> allEvents = getAllRegisteredEvents();
        if (allEvents.isEmpty()) {
            return Optional.empty();
        }

        List<WeightedEvent> weightedEvents = new ArrayList<>();
        double totalWeight = 0.0D;

        for (WrappedEvent wrappedEvent : allEvents.values()) {
            if (!wrappedEvent.configManager().isAutomaticStartEnabled()) {
                continue;
            }

            double chance = wrappedEvent.configManager().getChance();
            if (chance <= 0.0D) {
                continue;
            }

            totalWeight += chance;
            weightedEvents.add(new WeightedEvent(wrappedEvent.eventClass(), totalWeight));
        }

        if (weightedEvents.isEmpty() || totalWeight <= 0.0D) {
            return Optional.empty();
        }

        double randomValue = ThreadLocalRandom.current().nextDouble(totalWeight);
        for (WeightedEvent weightedEvent : weightedEvents) {
            if (randomValue < weightedEvent.cumulativeWeight()) {
                return Optional.of(weightedEvent.eventClass());
            }
        }

        return Optional.of(weightedEvents.get(weightedEvents.size() - 1).eventClass());
    }

    public Map<String, WrappedEvent> getEventsByPlugin(Plugin owner) {
        return Collections.unmodifiableMap(registeredEvents.getOrDefault(owner, Collections.emptyMap()));
    }

    public EventStartResult startEvent(String eventSpec) {
        return startEvent(eventSpec, null, null);
    }

    public EventStartResult startEvent(String eventSpec, Location fixedLocation) {
        return startEvent(eventSpec, fixedLocation, null);
    }

    public EventStartResult startEvent(String eventSpec, Location fixedLocation, String summonedBy) {
        ParsedEventSpec parsed = ParsedEventSpec.parse(eventSpec);
        if (parsed.eventId().isBlank()) {
            return EventStartResult.failure(EventStartError.UNKNOWN_EVENT, parsed.eventId(), parsed.rarityName());
        }

        Optional<WrappedEvent> wrappedOptional = getEventById(parsed.eventId());
        if (wrappedOptional.isEmpty()) {
            return EventStartResult.failure(EventStartError.UNKNOWN_EVENT, parsed.eventId(), parsed.rarityName());
        }

        try {
            Event event =
                    wrappedOptional.get().eventClass().getDeclaredConstructor().newInstance();
            String rarityName = parsed.rarityName();

            if (rarityName == null || rarityName.isBlank() || rarityName.equalsIgnoreCase("random")) {
                event.setRandomRarity();
            } else {
                Optional<EventRarity> rarity =
                        event.getConfigManager().getRarityManager().getRarity(rarityName);
                if (rarity.isEmpty()) {
                    return EventStartResult.failure(EventStartError.UNKNOWN_RARITY, parsed.eventId(), rarityName);
                }
                event.setEventRarity(rarity.get());
            }

            event.setSummonedBy(summonedBy);
            if (fixedLocation != null) {
                event.setEventLocation(fixedLocation);
            }

            event.activate();
            if (!event.isActivate()) {
                return EventStartResult.failure(EventStartError.START_FAILED, parsed.eventId(), rarityName);
            }
            return EventStartResult.success(event, parsed.eventId(), rarityName);
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to start event '" + eventSpec + "'", exception);
            return EventStartResult.failure(EventStartError.START_FAILED, parsed.eventId(), parsed.rarityName());
        }
    }

    public void registerActiveEvent(Event event) {
        if (event != null) {
            activeEvents.add(event);
        }
    }

    public void unregisterActiveEvent(Event event) {
        if (event != null) {
            activeEvents.remove(event);
        }
    }

    public Set<Event> getActiveEventsById(String id) {
        if (id == null) {
            return Collections.emptySet();
        }

        return activeEvents.stream()
                .filter(event -> event.getId().equalsIgnoreCase(id))
                .collect(Collectors.toSet());
    }

    public void stopAllActiveEvents() {
        new ArrayList<>(activeEvents).forEach(Event::updateStopping);
    }

    public void clearAllRegisteredEvents() {
        registeredEvents.clear();
    }

    private String normalizeId(String id) {
        return id.toLowerCase(Locale.ROOT);
    }

    public enum EventStartError {
        NONE,
        UNKNOWN_EVENT,
        UNKNOWN_RARITY,
        START_FAILED
    }

    private record WeightedEvent(Class<? extends Event> eventClass, double cumulativeWeight) {}

    private record ParsedEventSpec(String eventId, String rarityName) {
        private static ParsedEventSpec parse(String value) {
            if (value == null) {
                return new ParsedEventSpec("", null);
            }

            String[] parts = value.trim().split(":", 2);
            String eventId = parts.length > 0 ? parts[0].trim() : "";
            String rarity = parts.length > 1 ? parts[1].trim() : null;
            return new ParsedEventSpec(eventId, rarity);
        }
    }

    public record EventStartResult(Event event, EventStartError error, String eventId, String rarityName) {
        public static EventStartResult success(Event event, String eventId, String rarityName) {
            return new EventStartResult(event, EventStartError.NONE, eventId, rarityName);
        }

        public static EventStartResult failure(EventStartError error, String eventId, String rarityName) {
            return new EventStartResult(null, error, eventId, rarityName);
        }

        public boolean success() {
            return error == EventStartError.NONE && event != null;
        }
    }

    public record WrappedEvent(Class<? extends Event> eventClass, EventConfigManager configManager) {}
}
