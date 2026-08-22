package ru.loper.suneventmanager.listeners;

import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import ru.loper.suneventmanager.manager.SummonBlockManager;
import ru.loper.suneventmanager.summoner.block.model.SummonBlockDefinition;
import ru.loper.suneventmanager.summoner.block.service.SummonBlockService;

@RequiredArgsConstructor
public final class SummonBlockListener implements Listener {
    private final SummonBlockManager summonBlockManager;
    private final SummonBlockService summonBlockService;

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        SummonBlockDefinition definition = summonBlockManager.findDefinition(event.getItemInHand());
        if (definition == null) {
            return;
        }

        if (summonBlockManager.isCancelBlockPlace()) {
            event.setCancelled(true);
        }

        summonBlockService.use(
                event.getPlayer(),
                definition,
                event.getHand(),
                event.getBlockPlaced().getLocation(),
                event.getBlockAgainst().getType());
    }
}
