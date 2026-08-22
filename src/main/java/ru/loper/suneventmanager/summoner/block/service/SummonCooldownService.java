package ru.loper.suneventmanager.summoner.block.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;
import ru.loper.suneventmanager.summoner.block.model.SummonBlockDefinition;

public final class SummonCooldownService {
    private final Map<CooldownKey, Long> cooldowns = new HashMap<>();

    public long getSecondsLeft(Player player, SummonBlockDefinition definition) {
        if (player == null || definition == null || hasBypass(player, definition)) {
            return 0L;
        }

        CooldownKey key = key(player, definition);
        long expiresAt = cooldowns.getOrDefault(key, 0L);
        long now = System.currentTimeMillis();
        if (expiresAt <= now) {
            cooldowns.remove(key);
            return 0L;
        }
        return Math.max(1L, (expiresAt - now + 999L) / 1000L);
    }

    public void start(Player player, SummonBlockDefinition definition) {
        if (player == null || definition == null || hasBypass(player, definition)) {
            return;
        }
        if (definition.cooldownSeconds() <= 0L) {
            return;
        }

        long expiresAt = System.currentTimeMillis() + definition.cooldownSeconds() * 1000L;
        cooldowns.put(key(player, definition), expiresAt);
    }

    public void reset(Player player, SummonBlockDefinition definition) {
        if (player != null) {
            reset(player.getUniqueId(), definition);
        }
    }

    public void reset(UUID playerId, SummonBlockDefinition definition) {
        if (playerId == null || definition == null) {
            return;
        }
        cooldowns.remove(new CooldownKey(playerId, definition.cooldownGroup()));
    }

    public void clear() {
        cooldowns.clear();
    }

    private CooldownKey key(Player player, SummonBlockDefinition definition) {
        return new CooldownKey(player.getUniqueId(), definition.cooldownGroup());
    }

    private boolean hasBypass(Player player, SummonBlockDefinition definition) {
        String permission = definition.bypassPermission();
        return permission != null && !permission.isBlank() && player.hasPermission(permission);
    }

    private record CooldownKey(UUID playerId, String group) {}
}
