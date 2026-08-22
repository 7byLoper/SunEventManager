package ru.loper.suneventmanager.api.runnable;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bukkit.inventory.ItemStack;
import ru.loper.suncore.api.scheduler.CoreRunnable;
import ru.loper.suneventmanager.api.models.EventChest;

@RequiredArgsConstructor
public class ChestFillRunnable extends CoreRunnable {
    private final List<ItemStack> lootItems;
    private final EventChest eventChest;

    private int index;

    @Override
    public void run() {
        if (eventChest.isRemoved()) {
            cancel();
            return;
        }

        int batchSize = Math.max(1, eventChest.getChestsManager().getFillItemsPerPeriod());
        for (int added = 0; added < batchSize && index < lootItems.size(); added++) {
            int slot = eventChest.getRandomSlot();
            if (slot < 0) {
                finish();
                return;
            }

            eventChest.putLootItem(slot, lootItems.get(index));
            index++;
        }

        if (index >= lootItems.size()) {
            finish();
        }
    }

    private void finish() {
        eventChest.markAsFilled();
        cancel();
    }
}
