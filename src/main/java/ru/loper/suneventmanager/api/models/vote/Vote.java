package ru.loper.suneventmanager.api.models.vote;

import java.util.*;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.entity.Player;
import ru.loper.suneventmanager.SunEventManager;
import ru.loper.suneventmanager.api.models.Event;
import ru.loper.suneventmanager.config.PluginConfigManager;
import ru.loper.suneventmanager.menu.vote.VoteMenu;

public class Vote {
    @Getter
    private final PluginConfigManager configManager;

    private final Map<Event, EventVote> eventVotes = new LinkedHashMap<>();
    private final SunEventManager plugin;

    @Getter
    private final VoteMenu voteMenu;

    @Getter
    private int voteTimer;

    private boolean menuDirty;

    public Vote(Set<Event> events, SunEventManager plugin) {
        this.configManager = plugin.getConfigManager();
        this.plugin = plugin;
        this.voteTimer = configManager.getVoteDuration();

        for (Event event : events) {
            eventVotes.put(event, new EventVote(configManager));
        }

        voteMenu = new VoteMenu(this);
        menuDirty = true;
    }

    public int tick() {
        if (voteTimer > 0) {
            voteTimer--;
        }
        return voteTimer;
    }

    public boolean hasVoted(Player player) {
        String name = player.getName();
        return eventVotes.values().stream().anyMatch(vote -> vote.hasVote(name));
    }

    public boolean hasVoted(Player player, @NonNull Event event) {
        EventVote vote = eventVotes.get(event);
        return vote != null && vote.hasVote(player.getName());
    }

    public void addVote(Player player, Event event) {
        EventVote targetVote = eventVotes.get(event);
        if (targetVote == null) {
            return;
        }

        if (hasVoted(player)) {
            removeVote(player);
        }

        targetVote.playerVote(player);
        menuDirty = true;
    }

    public void removeVote(Player player) {
        eventVotes.values().forEach(vote -> vote.removeVote(player));
        menuDirty = true;
    }

    public Optional<Event> getLeadingEvent() {
        return eventVotes.entrySet().stream()
                .filter(entry -> entry.getValue().getTotalVotes() > 0)
                .max(Comparator.comparingInt(entry -> entry.getValue().getTotalVotes()))
                .map(Map.Entry::getKey);
    }

    public void stopVote() {
        Optional<Event> winnerOptional = getLeadingEvent();
        if (winnerOptional.isEmpty()) {
            plugin.getLogger().warning("Голосование завершено, но ни один ивент не выбран.");
            return;
        }

        Event winner = winnerOptional.get();
        winner.activate();
        if (!winner.isActivate()) {
            plugin.getLogger().warning("Победивший ивент '" + winner.getId() + "' не удалось запустить.");
            return;
        }

        if (configManager.isDebug()) {
            plugin.getLogger().info("Голосование завершено. Победил ивент: " + winner.getId());
        }
    }

    public void reset() {
        voteTimer = configManager.getVoteDuration();
        eventVotes.values().forEach(EventVote::resetVotes);
        menuDirty = true;
    }

    public boolean consumeMenuDirty() {
        boolean dirty = menuDirty;
        menuDirty = false;
        return dirty;
    }

    public Map<Event, EventVote> getEventVotes() {
        return Collections.unmodifiableMap(eventVotes);
    }
}
