package ru.loper.suneventmanager.config;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import ru.loper.suncore.api.config.CustomConfig;
import ru.loper.suncore.utils.Colorize;
import ru.loper.suneventmanager.api.modules.event.Event;
import ru.loper.suneventmanager.api.modules.event.rarity.EventRarityManager;
import ru.loper.suneventmanager.utils.TimeUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Getter
public class EventConfigManager {
    protected final Plugin plugin;
    protected EventRarityManager rarityManager;
    protected CustomConfig eventConfig;
    protected Vector regionRadius;
    protected Vector generatorCenter;

    protected World eventWorld;
    protected int generatorRadius;
    protected List<Biome> generatorBiomes;
    protected String biomesPropertyType;

    private int eventStartTime;
    private int eventPrestartTime;
    private int eventStopTime;

    private String infoMessagePrestart;
    private String infoMessageStart;
    private String infoMessageStop;

    private boolean region = true;

    public EventConfigManager(CustomConfig eventConfig, Plugin plugin) {
        this.eventConfig = eventConfig;
        this.plugin = plugin;
        loadSettings();
    }

    private void loadSettings() {
        loadGeneratorSettings();
        loadRegionSettings();
        loadRarityManager();
        loadInfoMessages();
        loadTimers();
    }

    protected void loadRarityManager() {
        rarityManager = new EventRarityManager(eventConfig, plugin);
    }

    private void loadGeneratorSettings() {
        ConfigurationSection generatorSettingsSection = eventConfig.getConfig().getConfigurationSection("generator_settings");
        if (generatorSettingsSection == null) {
            plugin.getLogger().severe("Ошибка загрузки настроек генератора для ивента");
            return;
        }

        String worldName = generatorSettingsSection.getString("world", "world");
        eventWorld = Bukkit.getWorld(worldName);
        if (eventWorld == null) {
            plugin.getLogger().severe("Мир " + worldName + " не найден! Проверьте конфиг.");
            eventWorld = Bukkit.getWorld("world");
        }
        generatorRadius = generatorSettingsSection.getInt("radius", 2000);

        ConfigurationSection centerSection = generatorSettingsSection.getConfigurationSection("center");
        if (centerSection != null) {
            int centerX = centerSection.getInt("x", 0);
            int centerZ = centerSection.getInt("z", 0);
            generatorCenter = new Vector(centerX, 0, centerZ);
        } else {
            generatorCenter = new Vector(0, 0, 0);
        }

        List<String> biomeNames = generatorSettingsSection.getStringList("biomes");
        generatorBiomes = biomeNames.stream()
                .map(biomeName -> {
                    try {
                        return Biome.valueOf(biomeName.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Неизвестный биом: " + biomeName);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        biomesPropertyType = generatorSettingsSection.getString("biomes_property_type", "blacklist");
    }

    private void loadInfoMessages() {
        ConfigurationSection infoSection = eventConfig.getConfig().getConfigurationSection("info_messages");
        if (infoSection == null) {
            plugin.getLogger().warning("Секция info_messages не найдена в конфиге!");
            infoMessagePrestart = "Пре-стартовое сообщение не задано";
            infoMessageStart = "Сообщение старта не задано";
            infoMessageStop = "Сообщение окончания не задано";
            return;
        }

        infoMessagePrestart = Colorize.parse(infoSection.getString("prestart", "Пре-стартовое сообщение не задано"));
        infoMessageStart = Colorize.parse(infoSection.getString("start", "Сообщение старта не задано"));
        infoMessageStop = Colorize.parse(infoSection.getString("stop", "Сообщение окончания не задано"));
    }

    private void loadRegionSettings() {
        ConfigurationSection regionSettingsSection = eventConfig.getConfig().getConfigurationSection("region_settings");
        if (regionSettingsSection == null) {
            region = false;
            return;
        }

        ConfigurationSection radiusSection = regionSettingsSection.getConfigurationSection("region_radius");
        if (radiusSection == null) {
            plugin.getLogger().severe("Ошибка загрузки радиуса региона для ивента");
            return;
        }

        int regionX = radiusSection.getInt("x", 10);
        int regionY = radiusSection.getInt("y", 10);
        int regionZ = radiusSection.getInt("z", 10);

        regionRadius = new Vector(regionX, regionY, regionZ);
    }

    private void loadTimers() {
        ConfigurationSection timersSection = eventConfig.getConfig().getConfigurationSection("timers");
        if (timersSection == null) {
            plugin.getLogger().severe("Ошибка загрузки таймеров для ивента");
            return;
        }

        eventPrestartTime = timersSection.getInt("prestart", 60);
        eventStartTime = timersSection.getInt("started", 120);
        eventStopTime = timersSection.getInt("stopping", 120);
    }

    public String getInfoMessageStart(Event event) {
        return infoMessageStart
                .replace("{time}", TimeUtils.formatSeconds(event.getEventTimer()))
                .replace("{x}", String.valueOf(event.getEventLocation().getBlockX()))
                .replace("{y}", String.valueOf(event.getEventLocation().getBlockY()))
                .replace("{z}", String.valueOf(event.getEventLocation().getBlockZ()));
    }

    public String getInfoMessageStop(Event event) {
        return infoMessageStop
                .replace("{time}", TimeUtils.formatSeconds(event.getEventTimer()))
                .replace("{x}", String.valueOf(event.getEventLocation().getBlockX()))
                .replace("{y}", String.valueOf(event.getEventLocation().getBlockY()))
                .replace("{z}", String.valueOf(event.getEventLocation().getBlockZ()));
    }

    public String getInfoMessagePrestart(Event event) {
        return infoMessagePrestart
                .replace("{time}", TimeUtils.formatSeconds(event.getEventTimer()))
                .replace("{x}", String.valueOf(event.getEventLocation().getBlockX()))
                .replace("{y}", String.valueOf(event.getEventLocation().getBlockY()))
                .replace("{z}", String.valueOf(event.getEventLocation().getBlockZ()));
    }

    public boolean isAllowedBiome(Biome biome) {
        if (biomesPropertyType.equalsIgnoreCase("whitelist")) {
            return generatorBiomes.contains(biome);
        }

        return !generatorBiomes.contains(biome);
    }
}