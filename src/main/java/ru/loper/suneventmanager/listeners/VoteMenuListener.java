package ru.loper.suneventmanager.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import ru.loper.suneventmanager.menu.vote.VoteMenu;

public class VoteMenuListener implements Listener {

    @EventHandler
    public void onOpen(InventoryOpenEvent event) {
        if (!(event.getInventory().getHolder() instanceof VoteMenu voteMenu)) {
            return;
        }

        voteMenu.onOpen(event);
    }
}
