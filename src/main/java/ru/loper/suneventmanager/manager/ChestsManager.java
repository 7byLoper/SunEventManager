package ru.loper.suneventmanager.manager;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import ru.loper.suneventmanager.SunEventManager;
import ru.loper.suneventmanager.api.models.Event;
import ru.loper.suneventmanager.api.models.EventChest;
import ru.loper.suneventmanager.api.models.EventRarity;
import ru.loper.suneventmanager.utils.LocationUtils;
import ru.loper.sunlootmanager.api.modules.Loot;

@Getter
public class ChestsManager {
    private final EventRarity eventRarity;
    private final Map<Event, Set<EventChest>> eventChests = new HashMap<>();
    private final Set<Vector> chestOffsets = new LinkedHashSet<>();

    private String chestTitle = "Сундук с ресурсами";
    private Material chestMaterial = Material.BLUE_SHULKER_BOX;
    private int chestSize = 27;
    private int minItems = 1;
    private int maxItems = chestSize;
    private long fillDelayTicks = 1L;
    private long fillPeriodTicks = 1L;
    private int fillItemsPerPeriod = 1;
    private Material schematicMaterial;
    private boolean restoreOriginalBlock;
    private boolean saveContainerContents;

    public ChestsManager(EventRarity eventRarity) {
        this.eventRarity = eventRarity;
        loadChestValues();
    }

    private void loadChestValues() {
        ConfigurationSection chestsSection = eventRarity.getChestsSection();
        if (chestsSection == null) {
            return;
        }

        chestTitle = chestsSection.getString("title", chestTitle);
        int rows = Math.max(1, Math.min(6, chestsSection.getInt("rows", 3)));
        chestSize = rows * 9;
        chestMaterial = getMaterialOrDefault(chestsSection.getString("material", "BLUE_SHULKER_BOX"));

        minItems = Math.max(0, Math.min(chestSize, chestsSection.getInt("items.min", 1)));
        maxItems = Math.max(minItems, Math.min(chestSize, chestsSection.getInt("items.max", chestSize)));
        fillDelayTicks = Math.max(0L, chestsSection.getLong("fill.delay_ticks", 1L));
        fillPeriodTicks = Math.max(1L, chestsSection.getLong("fill.period_ticks", 1L));
        fillItemsPerPeriod = Math.max(1, chestsSection.getInt("fill.items_per_period", 1));
        schematicMaterial = parseOptionalBlockMaterial(chestsSection.getString("schematic_material"));
        restoreOriginalBlock = chestsSection.getBoolean("restore_original_block", false);
        saveContainerContents = chestsSection.getBoolean("save_container_contents", restoreOriginalBlock);

        chestsSection.getStringList("offsets").stream()
                .map(LocationUtils::parseVectorFromString)
                .forEach(chestOffsets::add);
    }

    private Material parseOptionalBlockMaterial(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }

        Material material = Material.matchMaterial(name);
        return material != null && material.isBlock() ? material : null;
    }

    private Material getMaterialOrDefault(String name) {
        Material material = name == null ? null : Material.matchMaterial(name);
        if (!isContainerMaterial(material)) {
            SunEventManager.getInstance()
                    .getLogger()
                    .warning("Материал сундука '" + name + "' для редкости '" + eventRarity.getName()
                            + "' не является контейнером. Используется BLUE_SHULKER_BOX.");
            return Material.BLUE_SHULKER_BOX;
        }

        return material;
    }

    private boolean isContainerMaterial(Material material) {
        if (material == null) {
            return false;
        }

        return Tag.SHULKER_BOXES.isTagged(material)
                || switch (material) {
                    case CHEST, TRAPPED_CHEST, BARREL, HOPPER, DROPPER, DISPENSER, ENDER_CHEST -> true;
                    default -> false;
                };
    }

    public void loadChests(Event event) {
        loadChests(event, event.getEventLocation());
    }

    public void loadChests(Event event, Location location) {
        if (location == null) {
            return;
        }

        removeChests(event);
        Set<Location> chestLocations = new LinkedHashSet<>();
        for (Vector chestOffset : chestOffsets) {
            chestLocations.add(location.clone().add(chestOffset));
        }

        if (schematicMaterial != null && event.getSchematicManager() != null) {
            event.getSchematicManager().getLocationsByMaterial(schematicMaterial).stream()
                    .map(Location::clone)
                    .forEach(chestLocations::add);
        }

        Set<EventChest> chests = new LinkedHashSet<>();
        for (Location chestLocation : chestLocations) {
            EventChest chest = new EventChest(this, chestLocation, event);
            chest.spawn();
            chests.add(chest);
        }

        eventChests.put(event, chests);
    }

    public void removeChests(Event event) {
        Set<EventChest> chests = eventChests.remove(event);
        if (chests != null) {
            chests.forEach(EventChest::remove);
        }
    }

    public void openChests(Event event) {
        eventChests.getOrDefault(event, Collections.emptySet()).forEach(EventChest::open);
    }

    public void closeChests(Event event) {
        eventChests.getOrDefault(event, Collections.emptySet()).forEach(EventChest::close);
    }

    public Set<EventChest> getEventChests(Event event) {
        return Collections.unmodifiableSet(eventChests.getOrDefault(event, Collections.emptySet()));
    }

    public void updateHolograms(Event event) {
        eventChests.getOrDefault(event, Collections.emptySet()).forEach(EventChest::updateHologram);
    }

    public List<ItemStack> getRandomItems() {
        Loot loot = eventRarity.getLoot();
        if (loot == null) {
            return Collections.emptyList();
        }

        return loot.generateLoot(getGenerateItemsCount());
    }

    public int getGenerateItemsCount() {
        if (minItems == maxItems) {
            return minItems;
        }
        return ThreadLocalRandom.current().nextInt(minItems, maxItems + 1);
    }
}
