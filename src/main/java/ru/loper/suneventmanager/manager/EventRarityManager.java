package ru.loper.suneventmanager.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import ru.loper.suncore.api.config.CustomConfig;
import ru.loper.suneventmanager.api.models.EventRarity;
import ru.loper.sunlootmanager.SunLootManager;

@Getter
public class EventRarityManager {
    protected final Plugin plugin;
    protected final CustomConfig config;

    protected final Map<String, EventRarity> rarities = new HashMap<>();

    public EventRarityManager(CustomConfig config, Plugin plugin) {
        this.plugin = plugin;
        this.config = config;
    }

    public void loadRarities() {
        rarities.clear();
        ConfigurationSection raresSection = config.getConfig().getConfigurationSection("rarities");
        if (raresSection == null) {
            plugin.getLogger().severe("Раздел 'rarities' не найден в конфиге!");
            return;
        }

        for (String rarityKey : raresSection.getKeys(false)) {
            ConfigurationSection raritySection = raresSection.getConfigurationSection(rarityKey);
            if (raritySection == null) {
                continue;
            }

            plugin.getLogger().info("Successful load of rarity: " + rarityKey);
            rarities.put(rarityKey.toLowerCase(), createRarity(raritySection));
        }
    }

    protected @NotNull EventRarity createRarity(ConfigurationSection raritySection) {
        return new EventRarity(raritySection, SunLootManager.getInstance().getLootManager(), config);
    }

    public Optional<EventRarity> getRandomRarity() {
        if (rarities.isEmpty()) {
            plugin.getLogger().warning("Rarities is empty, please add rarities to config");
            return Optional.empty();
        }

        int totalChance =
                rarities.values().stream().mapToInt(EventRarity::getChance).sum();

        if (totalChance <= 0) {
            plugin.getLogger()
                    .warning("Total chance of rarities is " + totalChance + ". Using first rarity as fallback.");
            return rarities.values().stream().findFirst();
        }

        int randomValue = ThreadLocalRandom.current().nextInt(totalChance);
        int currentSum = 0;

        for (EventRarity rare : rarities.values()) {
            currentSum += rare.getChance();
            if (randomValue < currentSum) {
                return Optional.of(rare);
            }
        }

        plugin.getLogger().warning("Random rarity selection failed, returning last rarity");
        return rarities.values().stream().reduce((first, second) -> second);
    }

    public Optional<EventRarity> getRarity(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(rarities.get(name.toLowerCase(java.util.Locale.ROOT)));
    }

    public Set<String> getAvailableRarities() {
        return rarities.keySet();
    }
}
