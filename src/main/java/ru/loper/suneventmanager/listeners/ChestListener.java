package ru.loper.suneventmanager.listeners;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import ru.loper.suneventmanager.api.modules.chest.Chest;

public class ChestListener implements Listener {
    @EventHandler
    private void onInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK) || block == null) return;

        Chest chest = Chest.getChest(block);
        if (chest == null) return;

        chest.onInteract(event);
    }

    @EventHandler
    private void onClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();

        InventoryHolder holder = inventory.getHolder();
        if (holder == null) return;
        if (!(holder instanceof Chest chest)) return;

        chest.onClick(event);
    }
}
