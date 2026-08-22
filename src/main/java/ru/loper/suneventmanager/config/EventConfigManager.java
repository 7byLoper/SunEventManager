package ru.loper.suneventmanager.config;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import ru.loper.suncore.api.config.CustomConfig;
import ru.loper.suneventmanager.api.models.Event;
import ru.loper.suneventmanager.api.models.settings.EventBossBarSettings;
import ru.loper.suneventmanager.api.models.settings.GeneratorSettings;
import ru.loper.suneventmanager.manager.EventRarityManager;

@Getter
public class EventConfigManager {
    protected final Plugin plugin;
    private final double chance;
    private final String displayName;
    private final boolean automaticStartEnabled;
    private final boolean voteEnabled;
    private final boolean closeChestsOnStopping;

    @Setter
    protected EventRarityManager rarityManager;

    protected CustomConfig eventConfig;

    protected GeneratorSettings generatorSettings;
    private EventBossBarSettings bossBarSettings;

    private int eventStartTime;
    private int eventPrestartTime;
    private int eventOpenedTime;
    private int eventStopTime;
    private boolean openedStageEnabled;

    private boolean infoMessagesEnabled;
    private String infoMessagePrestart;
    private String infoMessageStart;
    private String infoMessageOpen;
    private String infoMessageStop;

    public EventConfigManager(CustomConfig eventConfig, Plugin plugin) {
        this.eventConfig = eventConfig;
        this.plugin = plugin;

        rarityManager = createRarityManager(eventConfig, plugin);
        if (rarityManager != null) {
            rarityManager.loadRarities();
        }

        loadGeneratorSettings();
        loadBossBarSettings();
        loadInfoMessages();
        loadTimers();

        chance = Math.max(0.0D, eventConfig.getConfig().getDouble("chance", 100.0D));
        displayName = eventConfig
                .getConfig()
                .getString("display_name", eventConfig.getConfig().getString("name", ""));
        automaticStartEnabled = eventConfig
                .getConfig()
                .getBoolean("automatic_start", eventConfig.getConfig().getBoolean("auto_start", true));
        voteEnabled = eventConfig.getConfig().getBoolean("vote_enabled", true);
        closeChestsOnStopping = eventConfig.getConfig().getBoolean("close_chests_on_stopping", false);
    }

    protected EventRarityManager createRarityManager(CustomConfig eventConfig, Plugin plugin) {
        return new EventRarityManager(eventConfig, plugin);
    }

    private void loadGeneratorSettings() {
        ConfigurationSection section = eventConfig.getConfig().getConfigurationSection("generator_settings");
        if (section == null) {
            plugin.getLogger().severe("Ошибка загрузки настроек генератора для ивента");
            return;
        }
        generatorSettings = GeneratorSettings.fromSection(section);
    }

    private void loadBossBarSettings() {
        bossBarSettings =
                EventBossBarSettings.fromSection(eventConfig.getConfig().getConfigurationSection("bossbar"));
    }

    private void loadInfoMessages() {
        ConfigurationSection infoSection = eventConfig.getConfig().getConfigurationSection("info_messages");
        if (infoSection == null) {
            plugin.getLogger().warning("Секция info_messages не найдена в конфиге!");
            infoMessagesEnabled = true;
            infoMessagePrestart = "Пре-стартовое сообщение не задано";
            infoMessageStart = "Сообщение старта не задано";
            infoMessageOpen = "Сообщение активной стадии не задано";
            infoMessageStop = "Сообщение окончания не задано";
            return;
        }

        infoMessagesEnabled = infoSection.getBoolean("enabled", true);
        infoMessagePrestart = infoSection.getString("prestart", "Пре-стартовое сообщение не задано");
        infoMessageStart = infoSection.getString("start", "Сообщение старта не задано");
        infoMessageOpen = infoSection.getString("open", infoSection.getString("active", infoMessageStart));
        infoMessageStop =
                infoSection.getString("stop", infoSection.getString("ending", "Сообщение окончания не задано"));
    }

    private void loadTimers() {
        ConfigurationSection timersSection = eventConfig.getConfig().getConfigurationSection("timers");
        if (timersSection == null) {
            plugin.getLogger().severe("Ошибка загрузки таймеров для ивента");
            eventPrestartTime = 60;
            eventStartTime = 120;
            eventOpenedTime = 0;
            eventStopTime = 120;
            openedStageEnabled = false;
            return;
        }

        eventPrestartTime = Math.max(0, timersSection.getInt("prestart", 60));
        eventStartTime = Math.max(0, timersSection.getInt("started", 120));

        openedStageEnabled = timersSection.contains("opened") || timersSection.contains("active");
        if (openedStageEnabled) {
            eventOpenedTime = Math.max(0, timersSection.getInt("opened", timersSection.getInt("active", 120)));
            eventStopTime = Math.max(0, timersSection.getInt("stopping", 0));
            return;
        }

        // Legacy lifecycle: STARTED -> STOPPING. The old `stopping` timer is the
        // active/open stage and therefore remains untouched for configs that do
        // not opt into the extra OPENED stage.
        eventOpenedTime = 0;
        eventStopTime = Math.max(0, timersSection.getInt("stopping", 120));
    }

    public String getInfoMessageStart(Event event) {
        return infoMessageStart;
    }

    public String getInfoMessageOpen(Event event) {
        return infoMessageOpen;
    }

    public String getInfoMessageStop(Event event) {
        return infoMessageStop;
    }

    public String getInfoMessagePrestart(Event event) {
        return infoMessagePrestart;
    }
}
