package ru.loper.suneventmanager.menu.setiings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.Nullable;
import ru.loper.suncore.api.config.CustomConfig;
import ru.loper.suncore.api.itemstack.ItemBuilder;
import ru.loper.suncore.api.menu.button.Button;
import ru.loper.suncore.api.menu.impl.AbstractMenu;
import ru.loper.suneventmanager.SunEventManager;
import ru.loper.suneventmanager.config.EventConfigManager;
import ru.loper.suneventmanager.config.PluginConfigManager;
import ru.loper.suneventmanager.manager.EventManager;

public class EventsMenu extends AbstractMenu {
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
        if (slots.isEmpty()) {
            return;
        }

        List<Map.Entry<String, EventManager.WrappedEvent>> eventEntries =
                new ArrayList<>(eventManager.getAllRegisteredEvents().entrySet());
        eventEntries.sort(Map.Entry.comparingByKey());
        int start = page * slots.size();

        ConfigurationSection formSection = config.getConfig().getConfigurationSection("event_form");
        if (formSection == null) return;

        for (int i = 0; i < slots.size() && start + i < eventEntries.size(); i++) {
            Map.Entry<String, EventManager.WrappedEvent> entry = eventEntries.get(start + i);
            String id = entry.getKey();
            int runs = eventManager.getActiveEventsById(id).size();

            ItemBuilder item = ItemBuilder.fromConfig(formSection)
                    .name(formSection.getString("display_name", "").replace("{event}", id))
                    .lore(formSection.getStringList("lore").stream()
                            .map(line -> line.replace("{event}", id).replace("{runs}", String.valueOf(runs)))
                            .toList());

            int slot = slots.get(i);
            menuButtons.add(new Button(item.build(), slot) {
                @Override
                public void onClick(InventoryClickEvent e) {
                    if (e.getWhoClicked() instanceof Player player) {
                        EventConfigManager eventConfigManager = entry.getValue().configManager();
                        new EventSettingsMenu(id, configManager, eventConfigManager, getInstance()).show(player);
                    }
                }
            });
        }
    }

    private void addNavigationButtons() {
        if (slots.isEmpty()) {
            return;
        }

        int total = eventManager.getAllRegisteredEvents().size();
        int maxPages = (int) Math.ceil((double) total / slots.size());

        ConfigurationSection itemsSection = config.getConfig().getConfigurationSection("items");
        if (itemsSection == null) return;

        ConfigurationSection nextPageSection = itemsSection.getConfigurationSection("next_page");
        if (nextPageSection != null && page + 1 < maxPages) {
            menuButtons.add(
                    new Button(ItemBuilder.fromConfig(nextPageSection).build(), nextPageSection.getInt("slot")) {
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
            menuButtons.add(
                    new Button(ItemBuilder.fromConfig(backPageSection).build(), backPageSection.getInt("slot")) {
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
