package ru.loper.suneventmanager.menu.setiings;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.Nullable;
import ru.loper.suncore.api.config.CustomConfig;
import ru.loper.suncore.api.gui.Button;
import ru.loper.suncore.api.gui.Menu;
import ru.loper.suncore.api.items.ItemBuilder;
import ru.loper.suneventmanager.SunEventManager;
import ru.loper.suneventmanager.api.modules.event.Event;
import ru.loper.suneventmanager.api.modules.event.EventManager;
import ru.loper.suneventmanager.config.EventConfigManager;
import ru.loper.suneventmanager.config.PluginConfigManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EventsMenu extends Menu {
    private final PluginConfigManager configManager;
    private final CustomConfig config;
    private final EventManager eventManager;
    private final List<Integer> slots;
    private int page = 0;

    public EventsMenu(PluginConfigManager configManager) {
        this.configManager = configManager;
        this.config = configManager.getEventsMenuConfig();
        this.eventManager = SunEventManager.getInstance().getEventManager();
        this.slots = config.getConfig().getIntegerList("events-slots");
    }

    @Override
    public @Nullable String getTitle() {
        return config.configMessage("title");
    }

    @Override
    public int getSize() {
        return config.getConfig().getInt("rows", 4) * 9;
    }

    @Override
    public void getItemsAndButtons() {
        addDecor();
        addEvents();
        addNavigationButtons();
    }

    private void addDecor() {
        ConfigurationSection decorSection = config.getConfig().getConfigurationSection("decor");
        if (decorSection == null) return;
        addDecorFromSection(decorSection);
    }

    private void addEvents() {
        List<Map.Entry<String, Class<? extends Event>>> eventEntries = new ArrayList<>(eventManager.getAllRegisteredEvents().entrySet());
        int start = page * slots.size();

        ConfigurationSection formSection = config.getConfig().getConfigurationSection("event_form");
        if (formSection == null) return;

        for (int i = 0; i < slots.size() && start + i < eventEntries.size(); i++) {
            Map.Entry<String, Class<? extends Event>> entry = eventEntries.get(start + i);
            String id = entry.getKey();
            int runs = eventManager.getActiveEventsById(id).size();

            ItemBuilder item = ItemBuilder.fromConfig(formSection)
                    .name(formSection.getString("display_name", "").replace("{event}", id))
                    .lore(formSection.getStringList("lore").stream()
                            .map(line -> line.replace("{event}", id).replace("{runs}", String.valueOf(runs)))
                            .toList());

            int slot = slots.get(i);
            buttons.add(new Button(item.build(), slot) {
                @Override
                public void onClick(InventoryClickEvent e) {
                    if (e.getWhoClicked() instanceof Player player) {
                        EventConfigManager eventConfigManager = SunEventManager.getInstance().getEventManager().getEventConfigById(id);
                        new EventSettingsMenu(id, configManager, eventConfigManager, getInstance()).show(player);
                    }
                }
            });
        }
    }

    private void addNavigationButtons() {
        int total = eventManager.getAllRegisteredEvents().size();
        int maxPages = (int) Math.ceil((double) total / slots.size());

        ConfigurationSection itemsSection = config.getConfig().getConfigurationSection("items");
        if (itemsSection == null) return;

        ConfigurationSection nextPageSection = itemsSection.getConfigurationSection("newt_page");
        if (nextPageSection != null && page + 1 < maxPages) {
            buttons.add(new Button(ItemBuilder.fromConfig(nextPageSection).build(), nextPageSection.getInt("slot")) {
                @Override
                public void onClick(InventoryClickEvent e) {
                    if (e.getWhoClicked() instanceof Player player) {
                        page++;
                        show(player);
                    }
                }
            });
        }

        ConfigurationSection backPageSection = itemsSection.getConfigurationSection("back_page");
        if (backPageSection != null && page > 0) {
            buttons.add(new Button(ItemBuilder.fromConfig(backPageSection).build(), backPageSection.getInt("slot")) {
                @Override
                public void onClick(InventoryClickEvent e) {
                    if (e.getWhoClicked() instanceof Player player) {
                        page--;
                        show(player);
                    }
                }
            });
        }
    }
}
