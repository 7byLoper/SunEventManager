package ru.loper.suneventmanager.api.modules.vote;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import ru.loper.suneventmanager.SunEventManager;
import ru.loper.suneventmanager.api.modules.event.Event;
import ru.loper.suneventmanager.api.modules.vote.runnable.VoteRunnable;

import java.util.*;

@Getter
public class VoteManager {
    private final SunEventManager plugin;
    private Vote currentVote;
    private VoteRunnable voteRunnable;

    public VoteManager(SunEventManager plugin) {
        this.plugin = plugin;
    }

    public void startVote() {
        if (currentVote != null) {
            plugin.getLogger().warning("Vote is already in progress!");
            return;
        }

        int voteEventsCount = plugin.getConfigManager().getRandomVoteCount();
        Map<String, Class<? extends Event>> all = plugin.getEventManager().getAllRegisteredEvents();

        if (all.isEmpty()) {
            plugin.getLogger().warning("No available events for voting.");
            return;
        }

        List<Map.Entry<String, Class<? extends Event>>> shuffled = new ArrayList<>(all.entrySet());
        Collections.shuffle(shuffled);

        Set<Event> voteEvents = new HashSet<>();
        for (int i = 0; i < Math.min(voteEventsCount, shuffled.size()); i++) {
            try {
                Event event = shuffled.get(i).getValue().getDeclaredConstructor().newInstance();
                voteEvents.add(event);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to instantiate event: " + shuffled.get(i).getKey());
                e.printStackTrace();
            }
        }

        if (voteEvents.isEmpty()) {
            plugin.getLogger().warning("Could not create any events for voting.");
            return;
        }

        currentVote = new Vote(voteEvents, plugin);
        voteRunnable = new VoteRunnable(this);
        voteRunnable.runTaskTimer(plugin, 20L, 20L);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
            plugin.getConfigManager().getVoteStartMessage().forEach(player::sendMessage);
        }

        if (SunEventManager.isDebug()) {
            plugin.getLogger().info("Vote has started between " + voteEvents.size() + " events.");
        }
    }

    public void clearCurrentVote() {
        if (voteRunnable != null && !voteRunnable.isCancelled()) {
            voteRunnable.cancel();
        }

        voteRunnable = null;
        currentVote = null;
    }

    public boolean isVoteInProgress() {
        return currentVote != null;
    }
}
