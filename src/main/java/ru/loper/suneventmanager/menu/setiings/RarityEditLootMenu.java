package ru.loper.suneventmanager.menu.setiings;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.inventory.InventoryClickEvent;
import ru.loper.suncore.api.gui.Menu;
import ru.loper.suneventmanager.api.modules.event.rarity.EventRarity;
import ru.loper.sunlootmanager.SunLootManager;
import ru.loper.sunlootmanager.api.manager.LootManager;
import ru.loper.sunlootmanager.api.modules.Loot;
import ru.loper.sunlootmanager.menu.LootsMenu;

public class RarityEditLootMenu extends LootsMenu {
    private final EventRarity eventRarity;

    public RarityEditLootMenu(Menu parent, LootManager lootManager, EventRarity eventRarity) {
        super(lootManager, SunLootManager.getInstance().getConfigManager());
        this.eventRarity = eventRarity;
        setParent(parent);
    }

    @Override
    public void onLootClick(InventoryClickEvent event, Loot loot) {
        eventRarity.setLoot(loot);
        event.getWhoClicked().closeInventory();
    }

    @Override
    public ConfigurationSection getLootFormSection() {
        return config.getConfig().getConfigurationSection("loot_change_form");
    }
}
