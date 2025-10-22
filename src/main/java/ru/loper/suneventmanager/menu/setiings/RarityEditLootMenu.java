package ru.loper.suneventmanager.menu.setiings;

import ru.loper.suneventmanager.api.modules.loot.LootManager;
import ru.loper.suneventmanager.config.PluginConfigManager;
import ru.loper.suneventmanager.menu.loot.LootsMenu;

public class RarityEditLootMenu extends LootsMenu {
    public RarityEditLootMenu(LootManager lootManager, PluginConfigManager configManager) {
        super(lootManager, configManager);
    }
}
