package ru.loper.suneventmanager.api.modules.chest;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import ru.loper.suncore.utils.Colorize;
import ru.loper.suneventmanager.api.modules.event.Event;
import ru.loper.suneventmanager.api.modules.event.rarity.EventRarity;
import ru.loper.suneventmanager.utils.LocationUtils;
import ru.loper.sunlootmanager.api.modules.Loot;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Getter
public class ChestsManager {
    protected final EventRarity eventRarity;
    protected final Map<Event, Set<Chest>> eventChests;
    protected final Set<Vector> chestOffsets;
    protected String chestTitle;
    protected Material chestMaterial;
    protected int chestSize, minItems, maxItems;

    public ChestsManager(EventRarity eventRarity) {
        this.eventRarity = eventRarity;
        this.eventChests = new HashMap<>();
        this.chestOffsets = new HashSet<>();
        loadChestValues();
    }

    protected void loadChestValues() {
        ConfigurationSection chestsSection = eventRarity.getChestsSection();
        if (chestsSection != null) {
            chestTitle = Colorize.parse(chestsSection.getString("title", ""));
            chestSize = chestsSection.getInt("rows", 3) * 9;
            chestMaterial = getMaterialOrDefault(chestsSection.getString("material", "BLUE_SHULKER_BOX"));
            minItems = chestsSection.getInt("items.min", 1);
            maxItems = chestsSection.getInt("items.max", chestSize);

            loadChestOffsets(chestsSection);
        } else {
            chestTitle = "";
            chestSize = 3 * 9;
            chestMaterial = Material.BLUE_SHULKER_BOX;
            minItems = 1;
            maxItems = chestSize;
        }
    }

    protected void loadChestOffsets(ConfigurationSection chestsSection) {
        ConfigurationSection offsetsSection = chestsSection.getConfigurationSection("offsets");
        if (offsetsSection == null) return;

        for (String key : offsetsSection.getKeys(false)) {
            ConfigurationSection offsetSection = offsetsSection.getConfigurationSection(key);
            if (offsetSection == null) continue;

            chestOffsets.add(LocationUtils.getVectorFromSection(offsetSection));
        }
    }

    protected Material getMaterialOrDefault(String name) {
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return Material.BLUE_SHULKER_BOX;
        }
    }

    public void loadChests(Event event) {
        Location eventLocation = event.getEventLocation();
        if (eventLocation == null) return;

        Set<Chest> chests = new HashSet<>();

        for (Vector chestOffset : chestOffsets) {
            Location chestLocation = eventLocation.clone().add(chestOffset);
            Chest chest = new Chest(this, chestLocation, event);
            chest.spawn();
            chests.add(chest);
        }

        eventChests.put(event, chests);
    }

    public void loadChests(Event event, Location location) {
        if (location == null) return;

        Set<Chest> chests = new HashSet<>();

        for (Vector chestOffset : chestOffsets) {
            Location chestLocation = location.clone().add(chestOffset);
            Chest chest = new Chest(this, chestLocation, event);
            chest.spawn();
            chests.add(chest);
        }

        eventChests.put(event, chests);
    }

    public void removeChests(Event event) {
        Set<Chest> chests = eventChests.remove(event);
        if (chests != null) {
            chests.forEach(Chest::remove);
        }
        eventChests.remove(event);
    }

    public void openChests(Event event) {
        eventChests.getOrDefault(event, Collections.emptySet()).forEach(Chest::open);
    }

    public Set<Chest> getEventChests(Event event) {
        return eventChests.getOrDefault(event, Collections.emptySet());
    }

    public void updateHolograms(Event event) {
        eventChests.getOrDefault(event, Collections.emptySet()).forEach(Chest::updateHologram);
    }

    public List<ItemStack> getRandomItems() {
        Loot loot = eventRarity.getLoot();
        if (loot == null) return Collections.emptyList();
        return loot.generateLoot(getGenerateItemsCount());
    }

    public int getGenerateItemsCount() {
        return ThreadLocalRandom.current().nextInt(minItems, maxItems + 1);
    }
}
