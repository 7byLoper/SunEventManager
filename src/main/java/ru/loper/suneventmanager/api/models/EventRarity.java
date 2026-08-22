package ru.loper.suneventmanager.api.models;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import java.util.Collections;
import java.util.List;
import lombok.Data;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import ru.loper.suncore.api.config.CustomConfig;
import ru.loper.suneventmanager.SunEventManager;
import ru.loper.suneventmanager.api.enums.HologramTarget;
import ru.loper.suneventmanager.api.models.settings.EventMessageSettings;
import ru.loper.suneventmanager.api.models.settings.RegionSettings;
import ru.loper.suneventmanager.api.models.settings.SoundSettings;
import ru.loper.suneventmanager.hook.WEHook;
import ru.loper.suneventmanager.hook.WGHook;
import ru.loper.suneventmanager.manager.ChestsManager;
import ru.loper.suneventmanager.utils.LocationUtils;
import ru.loper.sunlootmanager.api.manager.LootManager;
import ru.loper.sunlootmanager.api.modules.Loot;

@Data
public class EventRarity {
    protected final CustomConfig config;
    protected final ConfigurationSection rareSection;
    protected final ConfigurationSection defaultsSection;
    protected final ConfigurationSection chestsSection;

    protected final String name;
    protected final String displayName;
    protected final int chance;

    protected final String schematicFileName;
    protected final boolean ignoreAirBlocks;
    protected final Vector schemeOffset;
    protected final Vector hologramOffset;

    protected final List<String> hologramStart;
    protected final List<String> hologramOpen;
    protected final List<String> hologramStop;
    protected final List<String> prestartMessages;
    protected final List<String> startMessages;
    protected final List<String> openMessages;
    protected final List<String> endingMessages;
    protected final List<String> stopMessages;
    protected final boolean prestartMessagesEnabled;
    protected final boolean startMessagesEnabled;
    protected final boolean openMessagesEnabled;
    protected final boolean endingMessagesEnabled;
    protected final boolean stopMessagesEnabled;

    protected final SoundSettings prestartSound;
    protected final SoundSettings startSound;
    protected final SoundSettings openSound;
    protected final SoundSettings endingSound;
    protected final SoundSettings stopSound;
    protected final RegionSettings regionSettings;
    protected final boolean hologramSpawn;
    protected final HologramTarget hologramTarget;
    protected final ChestsManager chestsManager;
    protected Loot loot;

    public EventRarity(
            ConfigurationSection rareSection, @NotNull LootManager lootManager, @NotNull CustomConfig config) {
        this.rareSection = rareSection;
        this.config = config;
        this.defaultsSection = config.getConfig().getConfigurationSection("rarity_defaults");

        name = rareSection.getName();
        displayName = string("display_name", string("name", name));
        chance = Math.max(0, integer("chance", 100));
        schematicFileName = string("schematic_file_name", "");
        ignoreAirBlocks = bool("ignore_air_blocks", false);
        loot = lootManager.getLoot(string("loot_name", "")).orElse(null);

        ConfigurationSection hologramsSection = section("holograms");
        hologramStart = list(hologramsSection, "start");
        hologramOpen = list(hologramsSection, "open", "activated");
        hologramStop = list(hologramsSection, "stop", "destroy");
        hologramSpawn = bool("hologram_toggle", false);
        hologramTarget = HologramTarget.parse(string("hologram_target", "CHESTS"));

        ConfigurationSection messagesSection = section("messages");
        EventMessageSettings prestartMessageSettings = EventMessageSettings.load(messagesSection, "prestart");
        EventMessageSettings startMessageSettings = EventMessageSettings.load(messagesSection, "start", "spawn");
        EventMessageSettings openMessageSettings = EventMessageSettings.load(messagesSection, "open", "activated");
        EventMessageSettings endingMessageSettings = EventMessageSettings.load(messagesSection, "ending", "destroy");
        EventMessageSettings stopMessageSettings = EventMessageSettings.load(messagesSection, "stop", "removing");

        prestartMessages = prestartMessageSettings.lines();
        startMessages = startMessageSettings.lines();
        openMessages = openMessageSettings.lines();
        endingMessages = endingMessageSettings.lines();
        stopMessages = stopMessageSettings.lines();
        prestartMessagesEnabled = prestartMessageSettings.enabled();
        startMessagesEnabled = startMessageSettings.enabled();
        openMessagesEnabled = openMessageSettings.enabled();
        endingMessagesEnabled = endingMessageSettings.enabled();
        stopMessagesEnabled = stopMessageSettings.enabled();

        ConfigurationSection soundsSection = section("sounds");
        prestartSound = sound(soundsSection, "prestart");
        startSound = sound(soundsSection, "start");
        openSound = sound(soundsSection, "open", "activated");
        endingSound = sound(soundsSection, "ending", "destroy");
        stopSound = sound(soundsSection, "stop", "removing");

        String schematicOffsetValue = string("schematic_offset", string("scheme_offset", "0:0:0"));
        schemeOffset = LocationUtils.parseVectorFromString(schematicOffsetValue);
        hologramOffset = LocationUtils.parseVectorFromString(string("hologram_offset", "0:0:0"));

        regionSettings = RegionSettings.fromSection(section("region_settings"));
        chestsSection = section("chests");
        chestsManager = new ChestsManager(this);
    }

    private static List<String> list(ConfigurationSection section, String... paths) {
        if (section == null || paths == null) {
            return Collections.emptyList();
        }

        for (String path : paths) {
            if (section.isList(path)) {
                return List.copyOf(section.getStringList(path));
            }
        }
        return Collections.emptyList();
    }

    private static SoundSettings sound(ConfigurationSection section, String... paths) {
        if (section == null || paths == null) {
            return null;
        }

        for (String path : paths) {
            String value = section.getString(path);
            if (value != null && !value.isBlank()) {
                return SoundSettings.fromString(value);
            }
        }
        return null;
    }

    public boolean isChestHologram() {
        return hologramSpawn && (hologramTarget == HologramTarget.CHESTS || hologramTarget == HologramTarget.BOTH);
    }

    public boolean isEventHologram() {
        return hologramSpawn && (hologramTarget == HologramTarget.EVENT || hologramTarget == HologramTarget.BOTH);
    }

    public void broadcastPrestart(Event event) {
        broadcastWithSound(event, prestartMessagesEnabled, prestartMessages, prestartSound);
    }

    public void broadcastStart(Event event) {
        broadcastWithSound(event, startMessagesEnabled, startMessages, startSound);
    }

    public void broadcastOpen(Event event) {
        broadcastWithSound(event, openMessagesEnabled, openMessages, openSound);
    }

    public void broadcastEnding(Event event) {
        broadcastWithSound(event, endingMessagesEnabled, endingMessages, endingSound);
    }

    public void broadcastStop(Event event) {
        broadcastWithSound(event, stopMessagesEnabled, stopMessages, stopSound);
    }

    private void broadcastWithSound(Event event, boolean messagesEnabled, List<String> messages, SoundSettings sound) {
        if ((!messagesEnabled || messages == null || messages.isEmpty()) && sound == null) {
            return;
        }

        if (messagesEnabled && messages != null && !messages.isEmpty()) {
            SunEventManager.getInstance()
                    .getMessageService()
                    .broadcast(messages, event == null ? Collections.emptyMap() : event.getMessagePlaceholders());
        }

        if (sound != null) {
            org.bukkit.Bukkit.getOnlinePlayers().forEach(sound::play);
        }
    }

    private ConfigurationSection section(String path) {
        ConfigurationSection direct = rareSection.getConfigurationSection(path);
        if (direct != null) {
            return direct;
        }
        return defaultsSection == null ? null : defaultsSection.getConfigurationSection(path);
    }

    private String string(String path, String fallback) {
        if (rareSection.contains(path)) {
            return rareSection.getString(path, fallback);
        }
        return defaultsSection == null ? fallback : defaultsSection.getString(path, fallback);
    }

    private int integer(String path, int fallback) {
        if (rareSection.contains(path)) {
            return rareSection.getInt(path, fallback);
        }
        return defaultsSection == null ? fallback : defaultsSection.getInt(path, fallback);
    }

    private boolean bool(String path, boolean fallback) {
        if (rareSection.contains(path)) {
            return rareSection.getBoolean(path, fallback);
        }
        return defaultsSection == null ? fallback : defaultsSection.getBoolean(path, fallback);
    }

    public void setLoot(Loot loot) {
        this.loot = loot;
        rareSection.set("loot_name", loot == null ? null : loot.getName());
        config.saveConfig();
    }

    public Clipboard getClipboard() {
        if (schematicFileName == null || schematicFileName.isBlank()) {
            return null;
        }
        return WEHook.loadClipboard(schematicFileName, SunEventManager.getInstance());
    }

    public void createRegion(Event event) {
        if (!regionSettings.isEnable() || event.getEventLocation() == null) {
            return;
        }

        WGHook.create(regionName(event), regionSettings, event.getEventLocation());
    }

    public void removeRegion(Event event) {
        if (!regionSettings.isEnable()
                || event.getEventLocation() == null
                || event.getEventLocation().getWorld() == null) {
            return;
        }

        WGHook.remove(event.getEventLocation().getWorld(), regionName(event));
    }

    private String regionName(Event event) {
        return "event_%s_region_%s".formatted(event.getId(), event.getUniqueId());
    }
}
