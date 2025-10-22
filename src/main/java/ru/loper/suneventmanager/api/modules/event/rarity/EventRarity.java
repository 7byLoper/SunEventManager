package ru.loper.suneventmanager.api.modules.event.rarity;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import ru.loper.suncore.api.config.CustomConfig;
import ru.loper.suncore.utils.Colorize;
import ru.loper.suneventmanager.SunEventManager;
import ru.loper.suneventmanager.api.modules.chest.ChestsManager;
import ru.loper.suneventmanager.manager.SoundSettings;
import ru.loper.suneventmanager.utils.LocationUtils;
import ru.loper.sunlootmanager.api.manager.LootManager;
import ru.loper.sunlootmanager.api.modules.Loot;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Getter
public class EventRarity {
    protected CustomConfig config;
    protected ConfigurationSection rareSection;
    protected ConfigurationSection chestsSection;

    protected String name;
    protected String displayName;
    protected int chance;

    protected String schematicFileName;
    protected Vector schemeOffset;
    protected Vector hologramOffset;

    protected List<String> hologramStart;
    protected List<String> hologramStop;
    protected List<String> prestartMessages;
    protected List<String> startMessages;
    protected List<String> stopMessages;

    protected SoundSettings prestartSound;
    protected SoundSettings startSound;
    protected SoundSettings stopSound;

    protected Map<Flag<?>, String> flags;
    protected Loot loot;

    protected ChestsManager chestsManager;

    protected boolean hologramSpawn;

    public EventRarity(ConfigurationSection rareSection, @NotNull LootManager lootManager, @NotNull CustomConfig config) {
        this.rareSection = rareSection;
        this.config = config;
        this.name = rareSection.getName();
        this.displayName = Colorize.parse(rareSection.getString("display_name",""));
        this.chance = rareSection.getInt("chance", 100);
        this.schematicFileName = rareSection.getString("schematic_file_name");
        this.loot = lootManager.getLoot(rareSection.getString("loot_name", "")).orElse(null);

        ConfigurationSection hologramsSection = rareSection.getConfigurationSection("holograms");
        this.hologramStart = hologramsSection != null ? hologramsSection.getStringList("start") : Collections.emptyList();
        this.hologramStop = hologramsSection != null ? hologramsSection.getStringList("stop") : Collections.emptyList();
        this.hologramSpawn = rareSection.getBoolean("hologram_toggle", false);

        ConfigurationSection messagesSection = rareSection.getConfigurationSection("messages");
        this.prestartMessages = messagesSection != null ? Colorize.parse(messagesSection.getStringList("prestart")) : Collections.emptyList();
        this.startMessages = messagesSection != null ? Colorize.parse(messagesSection.getStringList("start")) : Collections.emptyList();
        this.stopMessages = messagesSection != null ? Colorize.parse(messagesSection.getStringList("stop")) : Collections.emptyList();

        ConfigurationSection soundsSection = rareSection.getConfigurationSection("sounds");
        this.prestartSound = soundsSection != null && soundsSection.getString("prestart") != null ? new SoundSettings(soundsSection.getString("prestart")) : null;
        this.startSound = soundsSection != null && soundsSection.getString("start") != null ? new SoundSettings(soundsSection.getString("start")) : null;
        this.stopSound = soundsSection != null && soundsSection.getString("stop") != null ? new SoundSettings(soundsSection.getString("stop")) : null;

        ConfigurationSection schematicOffsetsSection = rareSection.getConfigurationSection("schematic_offsets");
        schemeOffset = LocationUtils.getVectorFromSection(schematicOffsetsSection);

        ConfigurationSection hologramOffsetsSection = rareSection.getConfigurationSection("hologram_offset");
        hologramOffset = LocationUtils.getVectorFromSection(hologramOffsetsSection);

        this.chestsSection = rareSection.getConfigurationSection("chests");
        chestsManager = new ChestsManager(this);

        this.flags = initFlags(rareSection.getConfigurationSection("region_flags"));
    }

    private Map<Flag<?>, String> initFlags(ConfigurationSection flagsSection) {
        if (flagsSection == null) return Collections.emptyMap();
        Map<Flag<?>, String> flagsMap = new HashMap<>();
        flagsSection.getKeys(false).forEach(flag -> {
            Flag<?> wgFlag = WorldGuard.getInstance().getFlagRegistry().get(flag);
            if (wgFlag != null) flagsMap.put(wgFlag, flagsSection.getString(flag));
            else Bukkit.getLogger().warning("[EventRarity] Unknown WorldGuard flag: " + flag);
        });
        return flagsMap;
    }

    public void broadcastPrestart() {
        broadcastWithSound(prestartMessages, prestartSound);
    }

    public void broadcastStart(int x, int y, int z) {
        broadcastWithSound(startMessages.stream()
                .map(line -> line.replace("{x}", String.valueOf(x))
                        .replace("{y}", String.valueOf(y))
                        .replace("{z}", String.valueOf(z)))
                .collect(Collectors.toList()), startSound);
    }

    public void broadcastStop() {
        broadcastWithSound(stopMessages, stopSound);
    }

    private void broadcastWithSound(List<String> messages, SoundSettings sound) {
        if (messages.isEmpty() && sound == null) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            messages.stream().filter(message -> !message.isEmpty()).forEach(player::sendMessage);
            if (sound != null) sound.play(player);
        }
    }

    public void setLoot(Loot loot) {
        this.loot = loot;
        if (loot != null) {
            rareSection.set("loot_name", loot.getName());
            config.saveConfig();
        }
    }

    public Clipboard getClipboard() {
        if (schematicFileName == null || schematicFileName.isEmpty()) {
            Bukkit.getLogger().warning("[EventRarity] Schematic filename not specified for rarity: " + name);
            return null;
        }

        File file = new File(new File(SunEventManager.getInstance().getDataFolder(), "schematics"), schematicFileName);
        if (!file.exists()) {
            Bukkit.getLogger().warning("[EventRarity] Schematic file not found: " + file.getAbsolutePath());
            return null;
        }

        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format == null) {
            Bukkit.getLogger().warning("[EventRarity] Unknown schematic format for file: " + file.getName());
            return null;
        }

        try (FileInputStream fis = new FileInputStream(file); ClipboardReader reader = format.getReader(fis)) {
            return reader.read();
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[EventRarity] Failed to load schematic: " + file.getName(), e);
            return null;
        }
    }
}