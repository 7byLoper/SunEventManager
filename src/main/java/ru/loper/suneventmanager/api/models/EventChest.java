package ru.loper.suneventmanager.api.models;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.loper.suncore.api.hook.holograms.HologramHook;
import ru.loper.suncore.api.hook.holograms.HologramsServices;
import ru.loper.suneventmanager.SunEventManager;
import ru.loper.suneventmanager.api.runnable.ChestFillRunnable;
import ru.loper.suneventmanager.config.LootSettingsManager;
import ru.loper.suneventmanager.manager.ChestsManager;
import ru.loper.suneventmanager.utils.TimeUtils;

@Getter
public class EventChest implements InventoryHolder {
    private static final Map<String, EventChest> CHESTS = new ConcurrentHashMap<>();

    private final UUID chestUuid = UUID.randomUUID();
    private final ChestsManager chestsManager;
    private final Event event;
    private final Location hologramLocation;
    private final String hologramName;
    private final Location location;
    private final Inventory inventory;
    private final Map<Integer, ItemStack> hiddenLoot = new HashMap<>();
    private final Set<Material> visualCooldownMaterials = new HashSet<>();
    private final Map<UUID, Long> lastDelayMessage = new HashMap<>();

    private ChestFillRunnable fillRunnable;
    private boolean open;
    private boolean filled;
    private boolean removed;
    private long lootUnlockAtMillis;
    private BlockData originalBlockData;
    private ItemStack[] originalContainerContents;

    public EventChest(@NotNull ChestsManager chestsManager, @NotNull Location location, @NotNull Event event) {
        this.chestsManager = chestsManager;
        this.location = location.clone();
        this.event = event;
        this.hologramLocation = location.clone().add(event.getEventRarity().getHologramOffset());
        this.hologramName = String.format(
                "%s_%s_%s",
                event.getId(), event.getUniqueId(), chestUuid.toString().substring(0, 8));
        this.inventory = Bukkit.createInventory(
                this,
                chestsManager.getChestSize(),
                SunEventManager.getInstance()
                        .getMessageService()
                        .render(null, chestsManager.getChestTitle(), event.getMessagePlaceholders()));

        CHESTS.put(getChestId(), this);
    }

    public static @Nullable EventChest getChest(@NotNull Block block) {
        if (!(block.getState() instanceof TileState state)) {
            return null;
        }

        String chestId = state.getPersistentDataContainer().get(getChestKey(), PersistentDataType.STRING);
        return chestId == null ? null : CHESTS.get(chestId);
    }

    public static Collection<EventChest> getAllChests() {
        return Collections.unmodifiableCollection(CHESTS.values());
    }

    public static void removeAllChests() {
        new ArrayList<>(CHESTS.values()).forEach(EventChest::remove);
    }

    private static NamespacedKey getChestKey() {
        return new NamespacedKey(SunEventManager.getInstance(), "event_chest_uuid");
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public void spawn() {
        Block block = location.getBlock();
        captureOriginalBlock(block);
        block.setType(chestsManager.getChestMaterial());

        if (block.getState() instanceof TileState state) {
            state.getPersistentDataContainer().set(getChestKey(), PersistentDataType.STRING, getChestId());
            state.update(true, false);
        }

        if (event.getEventRarity().isChestHologram()) {
            updateHologram(event.getEventRarity().getHologramStart());
        }
    }

    public void updateHologram() {
        if (!event.getEventRarity().isChestHologram()) {
            return;
        }

        List<String> lines =
                switch (event.getCurrentState()) {
                    case STARTED -> event.getEventRarity().getHologramStart();
                    case OPENED -> event.getEventRarity().getHologramOpen();
                    case STOPPING ->
                        event.getConfigManager().isOpenedStageEnabled()
                                ? event.getEventRarity().getHologramStop()
                                : event.getEventRarity().getHologramOpen().isEmpty()
                                        ? event.getEventRarity().getHologramStop()
                                        : event.getEventRarity().getHologramOpen();
                    default -> Collections.emptyList();
                };

        if (!lines.isEmpty()) {
            updateHologram(lines);
        }
    }

    private void updateHologram(List<String> lines) {
        List<String> formattedLines =
                SunEventManager.getInstance().getMessageService().toLegacyLines(lines, event.getMessagePlaceholders());

        HologramHook holograms = HologramsServices.activeHook();
        if (holograms != null) {
            holograms.createHologram(hologramName, hologramLocation, formattedLines);
        }
    }

    public void remove() {
        if (removed) {
            return;
        }
        removed = true;

        if (fillRunnable != null) {
            if (!fillRunnable.isCancelled()) {
                fillRunnable.cancel();
            }
            fillRunnable = null;
        }

        if (event.getEventRarity().isChestHologram()) {
            try {
                HologramHook holograms = HologramsServices.activeHook();
                if (holograms != null) {
                    holograms.removeHologram(hologramName);
                }
            } catch (RuntimeException exception) {
                SunEventManager.getInstance()
                        .getLogger()
                        .warning("Не удалось удалить голограмму сундука " + hologramName + ": "
                                + exception.getMessage());
            }
        }

        new ArrayList<>(inventory.getViewers()).forEach(HumanEntity::closeInventory);
        hiddenLoot.clear();
        visualCooldownMaterials.clear();
        lastDelayMessage.clear();

        Block block = location.getBlock();
        EventChest registered = getChest(block);
        if (registered == this) {
            restoreOrRemoveBlock(block);
        }

        CHESTS.remove(getChestId());
    }

    private void captureOriginalBlock(Block block) {
        if (!chestsManager.isRestoreOriginalBlock()) {
            return;
        }

        originalBlockData = block.getBlockData().clone();
        if (!chestsManager.isSaveContainerContents()) {
            return;
        }

        BlockState state = block.getState();
        if (state instanceof Container container) {
            originalContainerContents = cloneContents(container.getInventory().getContents());
        }
    }

    private void restoreOrRemoveBlock(Block block) {
        if (!chestsManager.isRestoreOriginalBlock() || originalBlockData == null) {
            block.setType(Material.AIR);
            return;
        }

        block.setBlockData(originalBlockData, false);
        if (originalContainerContents == null) {
            return;
        }

        BlockState restoredState = block.getState();
        if (restoredState instanceof Container container) {
            container.getInventory().setContents(cloneContents(originalContainerContents));
            container.update(true, false);
        }
    }

    private ItemStack[] cloneContents(ItemStack[] contents) {
        ItemStack[] copy = new ItemStack[contents.length];
        for (int index = 0; index < contents.length; index++) {
            ItemStack item = contents[index];
            copy[index] = item == null ? null : item.clone();
        }
        return copy;
    }

    public void open() {
        if (open) {
            return;
        }

        open = true;
        LootSettingsManager lootSettings = getLootSettings();
        if (lootSettings.isTakeDelayEnabled() && lootSettings.getTakeDelaySeconds() > 0) {
            lootUnlockAtMillis =
                    System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(lootSettings.getTakeDelaySeconds());
        }

        List<ItemStack> lootItems = chestsManager.getRandomItems();
        if (lootItems.isEmpty()) {
            filled = true;
            return;
        }

        fillRunnable = new ChestFillRunnable(lootItems, this);
        fillRunnable.runTaskTimer(
                SunEventManager.getInstance(), chestsManager.getFillDelayTicks(), chestsManager.getFillPeriodTicks());
    }

    public void onPlayerOpen(Player player) {
        if (!open) {
            return;
        }

        player.openInventory(inventory);
        applyVisualCooldown(player);
    }

    public int getRandomSlot() {
        int size = inventory.getSize();
        int attempts = Math.min(size * 2, 128);

        for (int attempt = 0; attempt < attempts; attempt++) {
            int index = ThreadLocalRandom.current().nextInt(size);
            if (inventory.getItem(index) == null && !hiddenLoot.containsKey(index)) {
                return index;
            }
        }

        return inventory.firstEmpty();
    }

    public void putLootItem(int slot, @NotNull ItemStack lootItem) {
        if (removed || slot < 0 || slot >= inventory.getSize()) {
            return;
        }

        ItemStack realItem = lootItem.clone();
        visualCooldownMaterials.add(realItem.getType());

        LootSettingsManager lootSettings = getLootSettings();
        ItemStack shownItem = realItem;
        if (lootSettings.shouldHideLoot()) {
            hiddenLoot.put(slot, realItem);
            shownItem = lootSettings.createRandomHidingItem();
            visualCooldownMaterials.add(shownItem.getType());
        }

        inventory.setItem(slot, shownItem);
        if (isTakeLocked() && lootSettings.isVisualCooldownEnabled()) {
            for (HumanEntity viewer : inventory.getViewers()) {
                if (viewer instanceof Player player) {
                    applyCooldown(player, shownItem.getType());
                }
            }
        }
    }

    public void close() {
        if (!open) {
            return;
        }

        open = false;
        lootUnlockAtMillis = 0L;
        new ArrayList<>(inventory.getViewers()).forEach(HumanEntity::closeInventory);
    }

    public void markAsFilled() {
        filled = true;
    }

    public void onClick(@NotNull InventoryClickEvent clickEvent) {
        int topSize = inventory.getSize();
        int rawSlot = clickEvent.getRawSlot();
        InventoryAction action = clickEvent.getAction();

        if (rawSlot >= 0 && rawSlot < topSize && hiddenLoot.containsKey(rawSlot)) {
            clickEvent.setCancelled(true);
            revealSlot(rawSlot, clickEvent.getWhoClicked());
            return;
        }

        if (isTakeLocked() && canAffectEventLoot(clickEvent, topSize)) {
            clickEvent.setCancelled(true);
            if (clickEvent.getWhoClicked() instanceof Player player) {
                sendTakeDelayMessage(player);
                applyVisualCooldown(player);
            }
            return;
        }

        if (rawSlot >= 0 && rawSlot < topSize && isInsertionAction(action)) {
            clickEvent.setCancelled(true);
            return;
        }

        if (rawSlot >= topSize && clickEvent.isShiftClick()) {
            clickEvent.setCancelled(true);
            return;
        }

        if (!hiddenLoot.isEmpty() && action == InventoryAction.COLLECT_TO_CURSOR) {
            clickEvent.setCancelled(true);
        }
    }

    public void onDrag(@NotNull InventoryDragEvent dragEvent) {
        int topSize = inventory.getSize();
        if (dragEvent.getRawSlots().stream().anyMatch(slot -> slot < topSize)) {
            dragEvent.setCancelled(true);
        }
    }

    public void onInteract(@NotNull PlayerInteractEvent interactEvent) {
        onPlayerOpen(interactEvent.getPlayer());
        interactEvent.setCancelled(true);
    }

    public boolean isTakeLocked() {
        return lootUnlockAtMillis > System.currentTimeMillis();
    }

    public int getRemainingTakeDelaySeconds() {
        long remaining = lootUnlockAtMillis - System.currentTimeMillis();
        if (remaining <= 0L) {
            return 0;
        }
        return (int) Math.ceil(remaining / 1000.0D);
    }

    private void revealSlot(int slot, HumanEntity viewer) {
        ItemStack item = hiddenLoot.remove(slot);
        if (item == null) {
            return;
        }

        inventory.setItem(slot, item);
        visualCooldownMaterials.add(item.getType());
        if (viewer instanceof Player player && isTakeLocked()) {
            applyCooldown(player, item.getType());
        }
    }

    private boolean canAffectEventLoot(InventoryClickEvent event, int topSize) {
        int rawSlot = event.getRawSlot();
        if (rawSlot >= 0 && rawSlot < topSize) {
            ItemStack current = event.getCurrentItem();
            return current != null && current.getType() != Material.AIR;
        }

        return event.getAction() == InventoryAction.COLLECT_TO_CURSOR;
    }

    private boolean isInsertionAction(InventoryAction action) {
        return switch (action) {
            case PLACE_ALL, PLACE_ONE, PLACE_SOME, SWAP_WITH_CURSOR, HOTBAR_SWAP, HOTBAR_MOVE_AND_READD -> true;
            default -> false;
        };
    }

    private void sendTakeDelayMessage(Player player) {
        LootSettingsManager settings = getLootSettings();
        String message = settings.getTakeDelayMessage();
        if (message == null || message.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        long lastMessage = lastDelayMessage.getOrDefault(player.getUniqueId(), 0L);
        if (now - lastMessage < settings.getMessageIntervalMillis()) {
            return;
        }

        lastDelayMessage.put(player.getUniqueId(), now);
        SunEventManager.getInstance()
                .getMessageService()
                .send(
                        player,
                        message,
                        SunEventManager.getInstance()
                                .getMessageService()
                                .placeholders("time", TimeUtils.formatSeconds(getRemainingTakeDelaySeconds())));
    }

    private void applyVisualCooldown(Player player) {
        LootSettingsManager settings = getLootSettings();
        if (!settings.isVisualCooldownEnabled() || !isTakeLocked()) {
            return;
        }

        visualCooldownMaterials.forEach(material -> applyCooldown(player, material));
    }

    private void applyCooldown(Player player, Material material) {
        if (material == Material.AIR || !isTakeLocked()) {
            return;
        }

        int ticks = Math.max(1, getRemainingTakeDelaySeconds() * 20);
        player.setCooldown(material, ticks);
    }

    private LootSettingsManager getLootSettings() {
        return SunEventManager.getInstance().getLootSettingsManager();
    }

    private String getChestId() {
        return String.format(
                "chest_%s_%s_%d_%d_%d",
                event.getId(), event.getUniqueId(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }
}
