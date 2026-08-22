package ru.loper.suneventmanager.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import ru.loper.suncore.api.config.CustomConfig;
import ru.loper.suncore.api.itemstack.ItemBuilder;

@Getter
public class LootSettingsManager {
    private final CustomConfig config;
    private final List<ItemStack> hidingItems = new ArrayList<>();
    private boolean hidingEnabled;
    private boolean takeDelayEnabled;
    private int takeDelaySeconds;
    private boolean visualCooldownEnabled;
    private long messageIntervalMillis;
    private String takeDelayMessage;

    public LootSettingsManager(@NotNull CustomConfig config) {
        this.config = config;
        load();
    }

    public final void load() {
        loadHidingSettings();
        loadTakeDelaySettings();
    }

    private void loadHidingSettings() {
        hidingEnabled = config.getConfig().getBoolean("hidden_loot.enabled", false);
        hidingItems.clear();

        ConfigurationSection itemsSection = config.getConfig().getConfigurationSection("hidden_loot.items");
        if (itemsSection == null) {
            return;
        }

        for (String key : itemsSection.getKeys(false)) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
            if (itemSection == null) {
                continue;
            }

            ItemStack item = ItemBuilder.fromConfig(itemSection).build();
            if (item.getType() != Material.AIR) {
                hidingItems.add(item);
            }
        }

        if (hidingEnabled && hidingItems.isEmpty()) {
            hidingEnabled = false;
        }
    }

    private void loadTakeDelaySettings() {
        takeDelayEnabled = config.getConfig().getBoolean("take_delay.enabled", false);
        takeDelaySeconds = Math.max(0, config.getConfig().getInt("take_delay.seconds", 0));
        visualCooldownEnabled = config.getConfig().getBoolean("take_delay.visual_cooldown", true);
        messageIntervalMillis = Math.max(0L, config.getConfig().getLong("take_delay.message_interval_ms", 750L));
        takeDelayMessage = config.getConfig()
                .getString("take_delay.message", "&#FF2222▶&f Предметы можно будет забрать через &#FF2222{time}&f.");
    }

    public boolean shouldHideLoot() {
        return hidingEnabled && !hidingItems.isEmpty();
    }

    public @NotNull ItemStack createRandomHidingItem() {
        if (hidingItems.isEmpty()) {
            return new ItemStack(Material.BARRIER);
        }

        ItemStack template = hidingItems.get(ThreadLocalRandom.current().nextInt(hidingItems.size()));
        return template.clone();
    }

    public List<ItemStack> getHidingItems() {
        return Collections.unmodifiableList(hidingItems);
    }
}
