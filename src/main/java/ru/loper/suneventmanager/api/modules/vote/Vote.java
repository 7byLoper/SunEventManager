package ru.loper.suneventmanager.api.modules.vote;

import lombok.Getter;
import lombok.NonNull;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import ru.loper.suneventmanager.SunEventManager;
import ru.loper.suneventmanager.api.modules.event.Event;
import ru.loper.suneventmanager.config.PluginConfigManager;
import ru.loper.suneventmanager.menu.vote.VoteMenu;

import java.util.*;

public class Vote {
    @Getter
    private final PluginConfigManager configManager;
    private final Map<Event, EventVote> eventVotes;
    private final SunEventManager plugin;

    @Getter
    private final VoteMenu voteMenu;

    @Getter
    private int voteTimer;

    public Vote(Set<Event> events, SunEventManager plugin) {
        this.eventVotes = new HashMap<>();
        this.configManager = plugin.getConfigManager();
        this.plugin = plugin;
        this.voteTimer = configManager.getVoteDuration();

        for (Event event : events) {
            this.eventVotes.put(event, new EventVote(configManager));
        }

        voteMenu = new VoteMenu(this);
    }

    public int tick() {
        return --voteTimer;
    }

    public boolean hasVoted(Player player) {
        String name = player.getName();
        return eventVotes.values().stream().anyMatch(vote -> vote.hasVote(name));
    }

    public boolean hasVoted(Player player, @NonNull Event event) {
        String name = player.getName();
        return eventVotes.get(event).hasVote(name);
    }

    public void addVote(Player player, Event event) {
        if (!eventVotes.containsKey(event)) return;
        if (hasVoted(player)) removeVote(player);

        eventVotes.get(event).playerVote(player);
    }

    public void removeVote(Player player) {
        for (EventVote vote : eventVotes.values()) {
            vote.removeVote(player);
        }
    }

    public Optional<Event> getLeadingEvent() {
        return eventVotes.entrySet().stream()
                .max(Comparator.comparingInt(entry -> entry.getValue().getTotalVotes()))
                .map(Map.Entry::getKey);
    }

    public void stopVote() {
        Optional<Event> winnerOpt = getLeadingEvent();
        if (winnerOpt.isEmpty()) {
            plugin.getLogger().warning("Голосование завершено, но ни один ивент не выбран.");
            return;
        }

        Event winner = winnerOpt.get();
        winner.activate();
        plugin.getEventManager().registerActiveEvent(winner);
        voteMenu.getVoteViewers().forEach(HumanEntity::closeInventory);
        if (configManager.isDebug()) {
            plugin.getLogger().info("Голосование завершено. Победил ивент: " + winner.getId());
        }
    }

    public void reset() {
        voteTimer = configManager.getVoteDuration();
        eventVotes.values().forEach(EventVote::resetVotes);
    }

    public Map<Event, EventVote> getEventVotes() {
        return Collections.unmodifiableMap(eventVotes);
    }
}
