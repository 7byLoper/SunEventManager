package ru.loper.suneventmanager.api.modules.event.rarity;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import ru.loper.suncore.api.config.CustomConfig;
import ru.loper.sunlootmanager.SunLootManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Getter
public class EventRarityManager {
    protected final Map<String, EventRarity> rarities = new HashMap<>();
    protected final CustomConfig config;
    private final Plugin plugin;

    public EventRarityManager(CustomConfig config, Plugin plugin) {
        this.plugin = plugin;
        this.config = config;
        loadRarities();
    }

    protected void loadRarities() {
        ConfigurationSection raresSection = config.getConfig().getConfigurationSection("rarities");
        if (raresSection == null) {
            plugin.getLogger().severe("Раздел 'rarities' не найден в конфиге!");
            return;
        }

        for (String rarityKey : raresSection.getKeys(false)) {
            ConfigurationSection raritySection = raresSection.getConfigurationSection(rarityKey);
            if (raritySection != null) {
                rarities.put(rarityKey.toLowerCase(), new EventRarity(raritySection, SunLootManager.getInstance().getLootManager(), config));
            }
        }
    }

    public Optional<EventRarity> getRandomRarity() {
        if (rarities.isEmpty()) {
            return Optional.empty();
        }

        int totalChance = rarities.values().stream()
                .mapToInt(EventRarity::getChance)
                .sum();

        int randomValue = ThreadLocalRandom.current().nextInt(totalChance);
        int currentSum = 0;

        for (EventRarity rare : rarities.values()) {
            currentSum += rare.getChance();
            if (randomValue < currentSum) {
                return Optional.of(rare);
            }
        }

        return Optional.empty();
    }

    public Optional<EventRarity> getRarity(String name) {
        return Optional.ofNullable(rarities.get(name.toLowerCase()));
    }

    public Set<String> getAvailableRarities() {
        return rarities.keySet();
    }
}