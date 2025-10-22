package ru.loper.suneventmanager.api.modules.loot.chest.runnable;

import lombok.RequiredArgsConstructor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import ru.loper.suneventmanager.api.modules.loot.chest.Chest;

import java.util.List;

@RequiredArgsConstructor
public class ChestFillRunnable extends BukkitRunnable {
    private final List<ItemStack> lootItems;
    private final Chest chest;
    private int index = 0;

    @Override
    public void run() {
        if (index >= lootItems.size()) {
            cancel();
            return;
        }

        int slot = chest.getRandomSlot();
        if (slot != -1) {
            chest.getInventory().setItem(slot, lootItems.get(index++));
        }
    }
}
