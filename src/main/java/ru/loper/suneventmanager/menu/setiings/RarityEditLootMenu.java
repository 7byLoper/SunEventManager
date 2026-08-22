package ru.loper.suneventmanager.menu.setiings;

import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import ru.loper.suncore.api.itemstack.ItemBuilder;
import ru.loper.suncore.api.menu.button.Button;
import ru.loper.suncore.api.menu.impl.AbstractMenu;
import ru.loper.suneventmanager.api.models.EventRarity;
import ru.loper.sunlootmanager.SunLootManager;
import ru.loper.sunlootmanager.api.manager.LootManager;
import ru.loper.sunlootmanager.api.modules.Loot;
import ru.loper.sunlootmanager.menu.LootsMenu;

public class RarityEditLootMenu extends LootsMenu {
    private final EventRarity eventRarity;

    public RarityEditLootMenu(AbstractMenu parentMenu, LootManager lootManager, EventRarity eventRarity) {
        super(lootManager, SunLootManager.getInstance().getConfigManager());
        this.eventRarity = eventRarity;
        this.parentMenu = parentMenu;
    }

    @Override
    protected void addLootButton(ItemBuilder builder, List<Integer> slots, int index, Loot loot) {
        menuButtons.add(new Button(builder.build(), slots.get(index)) {
            @Override
            public void onClick(InventoryClickEvent event) {
                if (!(event.getWhoClicked() instanceof Player player)) {
                    return;
                }

                eventRarity.setLoot(loot);
                if (parentMenu != null) {
                    parentMenu.show(player);
                } else {
                    player.closeInventory();
                }
            }
        });
    }

    @Override
    public ConfigurationSection getLootFormSection() {
        return config.getConfig().getConfigurationSection("loot_change_form");
    }
}
