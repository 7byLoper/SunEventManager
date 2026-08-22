package ru.loper.suneventmanager.manager;

import java.util.*;
import java.util.logging.Level;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import ru.loper.suncore.api.sound.SoundPlayer;
import ru.loper.suneventmanager.SunEventManager;
import ru.loper.suneventmanager.api.models.Event;
import ru.loper.suneventmanager.api.models.vote.Vote;
import ru.loper.suneventmanager.api.runnable.VoteRunnable;

@Getter
@RequiredArgsConstructor
public class VoteManager {
    private final SunEventManager plugin;

    private Vote currentVote;
    private VoteRunnable voteRunnable;

    public void startVote() {
        if (currentVote != null) {
            plugin.getLogger().warning("Vote is already in progress!");
            return;
        }

        int voteEventsCount = plugin.getConfigManager().getRandomVoteCount();
        Map<String, EventManager.WrappedEvent> allEvents =
                plugin.getEventManager().getAllRegisteredEvents();
        List<Map.Entry<String, EventManager.WrappedEvent>> shuffled = allEvents.entrySet().stream()
                .filter(entry -> entry.getValue().configManager().isVoteEnabled())
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        if (shuffled.isEmpty()) {
            plugin.getLogger().warning("No available events for voting.");
            return;
        }

        Collections.shuffle(shuffled);

        Set<Event> voteEvents = new LinkedHashSet<>();
        for (int i = 0; i < Math.min(voteEventsCount, shuffled.size()); i++) {
            Map.Entry<String, EventManager.WrappedEvent> entry = shuffled.get(i);
            try {
                Event event =
                        entry.getValue().eventClass().getDeclaredConstructor().newInstance();
                event.setRandomRarity();
                voteEvents.add(event);
            } catch (Exception exception) {
                plugin.getLogger().log(Level.SEVERE, "Failed to instantiate event: " + entry.getKey(), exception);
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
            SoundPlayer.play(player, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
            plugin.getMessageService()
                    .sendLines(
                            player, plugin.getConfigManager().getVoteStartMessage(), java.util.Collections.emptyMap());
        }
    }

    public void clearCurrentVote() {
        if (voteRunnable != null && !voteRunnable.isCancelled()) {
            voteRunnable.cancel();
        }

        if (currentVote != null) {
            new ArrayList<>(currentVote.getVoteMenu().getVoteViewers()).forEach(HumanEntity::closeInventory);
        }

        voteRunnable = null;
        currentVote = null;
    }

    public boolean isVoteInProgress() {
        return currentVote != null;
    }
}
