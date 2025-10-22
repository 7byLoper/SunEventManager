package ru.loper.suneventmanager.api.modules.vote;

import org.bukkit.entity.Player;
import ru.loper.suneventmanager.config.PluginConfigManager;

import java.util.HashMap;
import java.util.Map;

public class EventVote {
    private final Map<String, Integer> playerVotes;
    private final PluginConfigManager configManager;

    public EventVote(PluginConfigManager configManager) {
        this.configManager = configManager;
        this.playerVotes = new HashMap<>();
    }

    public int getTotalVotes() {
        return playerVotes.values().stream().mapToInt(Integer::intValue).sum();
    }

    public void playerVote(Player player) {
        String name = player.getName();

        if (playerVotes.containsKey(name)) return;

        int weight = 1;
        if (player.hasPermission(configManager.getPremiumVotePermission())) {
            weight = configManager.getPremiumVoteWeight();
        }

        playerVotes.put(name, weight);
    }

    public boolean hasVote(String playerName) {
        return playerVotes.containsKey(playerName);
    }

    public void removeVote(Player player) {
        playerVotes.remove(player.getName());
    }

    public Map<String, Integer> getVotes() {
        return new HashMap<>(playerVotes);
    }

    public void resetVotes() {
        playerVotes.clear();
    }
}
