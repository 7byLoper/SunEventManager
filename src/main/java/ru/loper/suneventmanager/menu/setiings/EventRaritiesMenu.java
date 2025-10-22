package ru.loper.suneventmanager.menu.setiings;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.loper.suncore.api.config.CustomConfig;
import ru.loper.suncore.api.gui.Button;
import ru.loper.suncore.api.gui.Menu;
import ru.loper.suncore.api.items.ItemBuilder;
import ru.loper.suneventmanager.api.modules.event.rarity.EventRarity;
import ru.loper.suneventmanager.api.modules.event.rarity.EventRarityManager;
import ru.loper.suneventmanager.config.PluginConfigManager;
import ru.loper.sunlootmanager.SunLootManager;

import java.util.List;

public class EventRaritiesMenu extends Menu {
    private final CustomConfig config;
    private final List<Integer> slots;
    private final List<EventRarity> rarities;
    private final String eventId;
    private boolean back;
    private int page = 0;

    public EventRaritiesMenu(String eventId, EventRarityManager rarityManager, PluginConfigManager configManager, Menu parent) {
        this.config = configManager.getEventRaritiesMenuConfig();
        this.slots = config.getConfig().getIntegerList("rarities-slots");
        this.rarities = rarityManager.getRarities().values().stream().toList();
        this.eventId = eventId;
        this.back = true;
        setParent(parent);
    }

    @Override
    public @Nullable String getTitle() {
        return config.configMessage("title").replace("{event}", eventId);
    }

    @Override
    public int getSize() {
        return config.getConfig().getInt("rows", 4) * 9;
    }

    @Override
    public void getItemsAndButtons() {
        addDecor();
        addBackButton();
        addNextPageButton();
        addBackPageButton();
        addRarities();
    }

    private void addDecor() {
        ConfigurationSection decor = config.getConfig().getConfigurationSection("decor");
        if (decor != null) {
            addDecorFromSection(decor);
        }
    }

    private void addRarities() {
        ConfigurationSection rarityForm = config.getConfig().getConfigurationSection("rarity_form");
        if (rarityForm == null) return;

        int index = page * slots.size();
        for (int i = 0; i < slots.size() && index + i < rarities.size(); i++) {
            EventRarity rarity = rarities.get(index + i);

            ItemBuilder builder = ItemBuilder.fromConfig(rarityForm);
            builder.name(builder.name().replace("{rarity}", rarity.getName()))
                    .lore(builder.lore().stream()
                            .map(line -> line.replace("{rarity}", rarity.getName()))
                            .toList());

            int slot = slots.get(i);
            buttons.add(new Button(builder.build(), slot) {
                @Override
                public void onClick(InventoryClickEvent e) {
                    if (e.getWhoClicked() instanceof Player player) {
                        back = false;
                        new RarityEditLootMenu(getInstance(), SunLootManager.getInstance().getLootManager(), rarity)
                                .show(player);
                    }
                }
            });
        }
    }

    private void addNextPageButton() {
        int maxPages = (int) Math.ceil((double) rarities.size() / slots.size());
        if (page + 1 >= maxPages) return;

        ConfigurationSection section = config.getConfig().getConfigurationSection("items.next_page");
        if (section == null) return;

        buttons.add(new Button(ItemBuilder.fromConfig(section).build(), section.getInt("slot")) {
            @Override
            public void onClick(InventoryClickEvent e) {
                if (e.getWhoClicked() instanceof Player player) {
                    page++;
                    update(player);
                }
            }
        });
    }

    private void addBackPageButton() {
        if (page == 0) return;

        ConfigurationSection section = config.getConfig().getConfigurationSection("items.back_page");
        if (section == null) return;

        buttons.add(new Button(ItemBuilder.fromConfig(section).build(), section.getInt("slot")) {
            @Override
            public void onClick(InventoryClickEvent e) {
                if (e.getWhoClicked() instanceof Player player) {
                    page--;
                    update(player);
                }
            }
        });
    }

    private void addBackButton() {
        ConfigurationSection section = config.getConfig().getConfigurationSection("items.back");
        if (section == null) return;

        addReturnButton(section.getInt("slot"), ItemBuilder.fromConfig(section));
    }


    public void update(@NotNull Player player) {
        super.show(player);
        back = false;
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (getParent() == null) return;
        if (!back) {
            back = true;
            return;
        }
        getParent().show(player);
    }
}
