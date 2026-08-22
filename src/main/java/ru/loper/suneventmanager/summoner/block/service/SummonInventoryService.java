package ru.loper.suneventmanager.summoner.block.service;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import ru.loper.suneventmanager.manager.SummonBlockManager;
import ru.loper.suneventmanager.summoner.block.model.SummonBlockDefinition;

public final class SummonInventoryService {
    private final SummonBlockManager manager;

    public SummonInventoryService(SummonBlockManager manager) {
        this.manager = manager;
    }

    public ItemStack consume(Player player, EquipmentSlot hand) {
        if (player == null || player.getGameMode() == GameMode.CREATIVE) {
            return null;
        }

        ItemStack item = itemInHand(player, hand);
        if (item == null || item.getType().isAir()) {
            return null;
        }

        ItemStack consumedItem = item.clone();
        consumedItem.setAmount(1);
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
            return consumedItem;
        }

        setItemInHand(player, hand, null);
        return consumedItem;
    }

    public void refund(Player player, ItemStack item) {
        if (player == null || item == null) {
            return;
        }

        player.getInventory()
                .addItem(item.clone())
                .values()
                .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }

    public void refund(Location location, ItemStack item) {
        if (location == null || location.getWorld() == null || item == null) {
            return;
        }

        location.getWorld().dropItemNaturally(location, item.clone());
    }

    public void refund(Player player, SummonBlockDefinition definition) {
        if (player == null || definition == null) {
            return;
        }

        ItemStack item = manager.createItem(definition.id(), 1);
        if (item == null) {
            manager.getPlugin()
                    .getLogger()
                    .warning("Не удалось вернуть summon item '" + definition.id() + "': item недоступен.");
            return;
        }

        refund(player, item);
    }

    public void refund(Location location, SummonBlockDefinition definition) {
        if (location == null || location.getWorld() == null || definition == null) {
            return;
        }

        ItemStack item = manager.createItem(definition.id(), 1);
        if (item == null) {
            manager.getPlugin()
                    .getLogger()
                    .warning("Не удалось вернуть summon item '" + definition.id() + "': item недоступен.");
            return;
        }
        refund(location, item);
    }

    private ItemStack itemInHand(Player player, EquipmentSlot hand) {
        return hand == EquipmentSlot.OFF_HAND
                ? player.getInventory().getItemInOffHand()
                : player.getInventory().getItemInMainHand();
    }

    private void setItemInHand(Player player, EquipmentSlot hand, ItemStack item) {
        if (hand == EquipmentSlot.OFF_HAND) {
            player.getInventory().setItemInOffHand(item);
            return;
        }
        player.getInventory().setItemInMainHand(item);
    }
}
