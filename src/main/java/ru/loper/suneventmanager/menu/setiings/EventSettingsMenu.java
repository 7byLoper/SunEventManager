package ru.loper.suneventmanager.menu.setiings;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.loper.suncore.api.config.CustomConfig;
import ru.loper.suncore.api.itemstack.ItemBuilder;
import ru.loper.suncore.api.menu.button.Button;
import ru.loper.suncore.api.menu.impl.AbstractMenu;
import ru.loper.suneventmanager.SunEventManager;
import ru.loper.suneventmanager.api.models.Event;
import ru.loper.suneventmanager.config.EventConfigManager;
import ru.loper.suneventmanager.config.PluginConfigManager;
import ru.loper.suneventmanager.manager.EventManager;
import ru.loper.suneventmanager.message.ConfiguredMessages;

public class EventSettingsMenu extends AbstractMenu {
    private final CustomConfig config;
    private final String eventId;

    private final PluginConfigManager configManager;
    private final EventConfigManager eventConfigManager;

    private boolean back;

    public EventSettingsMenu(
            String eventId,
            PluginConfigManager configManager,
            EventConfigManager eventConfigManager,
            AbstractMenu parentMenu) {
        this.config = configManager.getEventSettingsMenuConfig();
        this.eventId = eventId;

        this.configManager = configManager;
        this.eventConfigManager = eventConfigManager;

        this.back = true;
        this.parentMenu = parentMenu;
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
        addRaritiesSettingsButton();
        addStopAllEventsButton();
        addStartEventButton();
    }

    private void addDecor() {
        ConfigurationSection decor = config.getConfig().getConfigurationSection("decor");
        if (decor != null) {
            addDecorFromSection(decor);
        }
    }

    private void addRaritiesSettingsButton() {
        ConfigurationSection section = config.getConfig().getConfigurationSection("items.rarities_settings");
        if (section == null) return;

        menuButtons.add(new Button(ItemBuilder.fromConfig(section).build(), section.getInt("slot")) {
            @Override
            public void onClick(InventoryClickEvent e) {
                if (e.getWhoClicked() instanceof Player player) {
                    EventRaritiesMenu menu = new EventRaritiesMenu(
                            eventId, eventConfigManager.getRarityManager(), configManager, EventSettingsMenu.this);

                    back = false;
                    menu.show(player);
                }
            }
        });
    }

    private void addStartEventButton() {
        ConfigurationSection section = config.getConfig().getConfigurationSection("items.start_event");
        if (section == null) {
            return;
        }

        menuButtons.add(new Button(ItemBuilder.fromConfig(section).build(), section.getInt("slot")) {
            @Override
            public void onClick(InventoryClickEvent event) {
                if (!(event.getWhoClicked() instanceof Player player)) {
                    return;
                }
                EventManager eventManager = SunEventManager.getInstance().getEventManager();
                EventManager.EventStartResult result = eventManager.startEvent(eventId + ":random");
                if (result.success()) {
                    ConfiguredMessages.send(
                            player,
                            "admin_menu.start_success",
                            "&a ▶ &fИвент инициализирован! UUID: {uuid}",
                            "uuid",
                            result.event().getUniqueId());
                } else {
                    ConfiguredMessages.send(
                            player,
                            "admin_menu.start_error",
                            "&c ▶ &fОшибка запуска ивента: {error}",
                            "error",
                            result.error().name());
                }
            }
        });
    }

    private void addStopAllEventsButton() {
        ConfigurationSection section = config.getConfig().getConfigurationSection("items.stop_all_events");
        if (section == null) return;

        menuButtons.add(new Button(ItemBuilder.fromConfig(section).build(), section.getInt("slot")) {
            @Override
            public void onClick(InventoryClickEvent event) {
                if (!(event.getWhoClicked() instanceof Player player)) {
                    return;
                }

                EventManager eventManager = SunEventManager.getInstance().getEventManager();
                eventManager.getActiveEventsById(eventId).forEach(Event::updateStopping);
                ConfiguredMessages.send(player, "admin_menu.stop_success", "&a ▶ &fВсе ивенты успешно остановлены. ");
            }
        });
    }

    private void addBackButton() {
        ConfigurationSection section = config.getConfig().getConfigurationSection("items.back");
        if (section == null) {
            return;
        }

        addReturnButton(section.getInt("slot"), ItemBuilder.fromConfig(section));
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player) || parentMenu == null) {
            return;
        }

        if (!back) {
            back = true;
            return;
        }

        Bukkit.getScheduler().runTask(SunEventManager.getInstance(), () -> {
            if (player.isOnline() && player.getOpenInventory().getTopInventory() != parentMenu.getInventory()) {
                parentMenu.show(player);
            }
        });
    }
}
