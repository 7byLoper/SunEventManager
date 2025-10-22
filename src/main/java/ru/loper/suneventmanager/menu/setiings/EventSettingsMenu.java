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
import ru.loper.suncore.utils.Colorize;
import ru.loper.suneventmanager.SunEventManager;
import ru.loper.suneventmanager.api.modules.event.Event;
import ru.loper.suneventmanager.api.modules.event.EventManager;
import ru.loper.suneventmanager.config.EventConfigManager;
import ru.loper.suneventmanager.config.PluginConfigManager;

public class EventSettingsMenu extends Menu {
    private final CustomConfig config;
    private final String eventId;
    private final PluginConfigManager configManager;
    private final EventConfigManager eventConfigManager;
    private boolean back;

    public EventSettingsMenu(String eventId, PluginConfigManager configManager, EventConfigManager eventConfigManager, Menu parent) {
        this.eventId = eventId;
        this.configManager = configManager;
        this.eventConfigManager = eventConfigManager;
        this.config = configManager.getEventSettingsMenuConfig();
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

        buttons.add(new Button(ItemBuilder.fromConfig(section).build(), section.getInt("slot")) {
            @Override
            public void onClick(InventoryClickEvent e) {
                if (e.getWhoClicked() instanceof Player player) {
                    EventRaritiesMenu menu = new EventRaritiesMenu(
                            eventId,
                            eventConfigManager.getRarityManager(),
                            configManager,
                            EventSettingsMenu.this
                    );

                    back = false;
                    menu.show(player);
                }
            }
        });
    }

    private void addStartEventButton() {
        ConfigurationSection section = config.getConfig().getConfigurationSection("items.start_event");
        if (section == null) return;

        buttons.add(new Button(ItemBuilder.fromConfig(section).build(), section.getInt("slot")) {
            @Override
            public void onClick(InventoryClickEvent event) {
                if (!(event.getWhoClicked() instanceof Player player)) {
                    return;
                }
                EventManager eventManager = SunEventManager.getInstance().getEventManager();
                eventManager.getEventById(eventId).ifPresentOrElse(eventClass -> {
                    try {
                        Event newEvent = eventClass.getDeclaredConstructor().newInstance();
                        newEvent.activate();
                        player.sendMessage(Colorize.parse("&c ▶ &fИвент инициализирован! UUID: " + newEvent.getUuid()));
                    } catch (Exception ex) {
                        player.sendMessage(Colorize.parse("&c ▶ &fОшибка запуска ивента. " + ex.getMessage()));
                    }
                }, () -> player.sendMessage(Colorize.parse("&c ▶ &fНет зарегистрированных ивентов.")));
            }
        });
    }

    private void addStopAllEventsButton() {
        ConfigurationSection section = config.getConfig().getConfigurationSection("items.stop_all_events");
        if (section == null) return;

        buttons.add(new Button(ItemBuilder.fromConfig(section).build(), section.getInt("slot")) {
            @Override
            public void onClick(InventoryClickEvent event) {
                if (!(event.getWhoClicked() instanceof Player player)) {
                    return;
                }

                EventManager eventManager = SunEventManager.getInstance().getEventManager();
                eventManager.getActiveEventsById(eventId).forEach(Event::updateStopping);
                player.sendMessage(Colorize.parse("&a ▶ &fВсе ивенты успешно остановлены. "));
            }
        });
    }


    private void addBackButton() {
        ConfigurationSection section = config.getConfig().getConfigurationSection("items.back");
        if (section == null) return;

        addReturnButton(section.getInt("slot"), ItemBuilder.fromConfig(section));
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