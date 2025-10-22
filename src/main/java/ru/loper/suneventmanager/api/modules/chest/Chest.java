package ru.loper.suneventmanager.api.modules.chest;

import com.jeff_media.customblockdata.CustomBlockData;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.loper.suneventmanager.SunEventManager;
import ru.loper.suneventmanager.api.modules.chest.runnable.ChestFillRunnable;
import ru.loper.suneventmanager.api.modules.event.Event;
import ru.loper.suneventmanager.hook.HologramsHook;
import ru.loper.suneventmanager.utils.TimeUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Getter
public class Chest implements InventoryHolder {
    protected static final Map<String, Chest> CHESTS = new ConcurrentHashMap<>();
    protected static final NamespacedKey CHEST_UUID_KEY = new NamespacedKey(SunEventManager.getInstance(), "chest-uuid-key");

    protected final ChestsManager chestsManager;
    protected final Location hologramLocation;
    protected final Location location;
    protected final Event event;
    protected final String hologramName;
    protected final Inventory inventory;
    protected final UUID chestUuid = UUID.randomUUID();
    protected boolean open;
    protected boolean filled;

    public Chest(ChestsManager chestsManager, Location location, Event event) {
        this.chestsManager = Objects.requireNonNull(chestsManager, "ChestsManager cannot be null");
        this.location = Objects.requireNonNull(location, "Location cannot be null").clone();
        this.event = Objects.requireNonNull(event, "Event cannot be null");

        this.hologramLocation = location.clone().add(event.getEventRarity().getHologramOffset());
        this.hologramName = String.format("%s_%s_%s", event.getId(), event.getUuid(), chestUuid.toString().substring(0, 8));
        this.inventory = Bukkit.createInventory(this, chestsManager.getChestSize(),
                Component.text(chestsManager.getChestTitle()));

        CHESTS.put(getChestId(), this);
    }

    public static @Nullable Chest getChest(@NotNull Block block) {
        CustomBlockData customBlockData = new CustomBlockData(block, SunEventManager.getInstance());
        String chestId = customBlockData.get(CHEST_UUID_KEY, PersistentDataType.STRING);
        return chestId != null ? CHESTS.get(chestId) : null;
    }

    public static Collection<Chest> getAllChests() {
        return Collections.unmodifiableCollection(CHESTS.values());
    }

    public static void removeAllChests() {
        new ArrayList<>(CHESTS.values()).forEach(Chest::remove);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public void spawn() {
        Block block = location.getBlock();
        block.setType(chestsManager.getChestMaterial());

        CustomBlockData customBlockData = new CustomBlockData(block, SunEventManager.getInstance());
        customBlockData.set(CHEST_UUID_KEY, PersistentDataType.STRING, getChestId());

        if (!event.getEventRarity().isHologramSpawn()) return;
        updateHologram(event.getEventRarity().getHologramStart());
    }

    public void updateHologram() {
        if (!event.getEventRarity().isHologramSpawn()) return;

        List<String> lines = switch (event.getCurrentState()) {
            case STARTED -> event.getEventRarity().getHologramStart();
            case STOPPING -> event.getEventRarity().getHologramStop();
            default -> Collections.emptyList();
        };

        if (!lines.isEmpty()) {
            updateHologram(lines);
        }
    }

    protected void updateHologram(List<String> lines) {
        List<String> formattedLines = lines.stream()
                .map(line -> line.replace("{time}", TimeUtils.formatSeconds(event.getEventTimer())))
                .collect(Collectors.toList());

        HologramsHook.createOrUpdateHologram(formattedLines, hologramLocation, hologramName);
    }

    public void remove() {
        HologramsHook.remove(hologramName);

        Block block = location.getBlock();
        block.setType(Material.AIR);

        CustomBlockData customBlockData = new CustomBlockData(block, SunEventManager.getInstance());
        customBlockData.remove(CHEST_UUID_KEY);

        new ArrayList<>(inventory.getViewers()).forEach(HumanEntity::closeInventory);

        CHESTS.remove(getChestId());
    }

    public void open() {
        if (open) return;

        this.open = true;
        List<ItemStack> lootItems = chestsManager.getRandomItems();

        if (lootItems.isEmpty()) {
            filled = true;
            return;
        }

        new ChestFillRunnable(lootItems, this)
                .runTaskTimer(SunEventManager.getInstance(), 1L, 1L);
    }

    public void onPlayerOpen(Player player) {
        if (!isOpen()) return;
        player.openInventory(getInventory());
    }

    public int getRandomSlot() {
        int size = inventory.getSize();
        int attempts = 0;
        int maxAttempts = size * 2;

        while (attempts++ < maxAttempts) {
            int index = ThreadLocalRandom.current().nextInt(size);
            if (inventory.getItem(index) == null) {
                return index;
            }
        }

        return inventory.firstEmpty();
    }

    public void markAsFilled() {
        this.filled = true;
    }

    protected String getChestId() {
        return String.format("chest_%s_%s_%d_%d_%d",
                event.getId(),
                event.getUuid(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ());
    }

    public void onClick(InventoryClickEvent event) {

    }

    public void onInteract(@NotNull PlayerInteractEvent event) {
        onPlayerOpen(event.getPlayer());
        event.setCancelled(true);
    }
}