package ru.loper.suneventmanager.summoner.block.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import ru.loper.suncore.api.config.CustomConfig;
import ru.loper.suncore.api.itemstack.ItemBuilder;
import ru.loper.suneventmanager.summoner.block.model.*;

public final class SummonBlockConfigLoader {
    private final Plugin plugin;
    private final SummonConfigValueReader values;
    private final SummonEffectsConfigLoader effectsLoader;

    public SummonBlockConfigLoader(Plugin plugin) {
        this.plugin = plugin;
        this.values = new SummonConfigValueReader();
        this.effectsLoader = new SummonEffectsConfigLoader(plugin, values);
    }

    public SummonBlockSettings load(CustomConfig customConfig) {
        if (customConfig == null || customConfig.getConfig() == null) {
            plugin.getLogger().severe("summon_blocks.yml недоступен. Механика призыва отключена.");
            return disabledSettings();
        }

        ConfigurationSection root = customConfig.getConfig();
        SummonMessages messages = loadMessages(root.getConfigurationSection("messages"), null);
        Map<String, SummonBlockDefinition> definitions = loadDefinitions(
                root.getConfigurationSection("items"),
                root.getConfigurationSection("profiles"),
                root.getConfigurationSection("defaults"),
                messages,
                "summon_blocks.yml");

        return new SummonBlockSettings(
                root.getBoolean("enabled", false),
                root.getBoolean("cancel_block_place", true),
                root.getBoolean("consume_item", true),
                messages,
                Collections.unmodifiableMap(definitions));
    }

    public Map<String, SummonBlockDefinition> loadExternal(
            ConfigurationSection root, SummonMessages fallbackMessages, String sourceName) {
        if (root == null || !root.getBoolean("enabled", true)) {
            return Map.of();
        }

        SummonMessages messages = loadMessages(root.getConfigurationSection("messages"), fallbackMessages);
        return Collections.unmodifiableMap(loadDefinitions(
                root.getConfigurationSection("items"),
                root.getConfigurationSection("profiles"),
                root.getConfigurationSection("defaults"),
                messages,
                sourceName == null ? "event config" : sourceName));
    }

    private Map<String, SummonBlockDefinition> loadDefinitions(
            ConfigurationSection items,
            ConfigurationSection profiles,
            ConfigurationSection defaults,
            SummonMessages messages,
            String sourceName) {
        Map<String, SummonBlockDefinition> result = new LinkedHashMap<>();
        if (items == null) {
            plugin.getLogger().warning(sourceName + ": секция items отсутствует.");
            return result;
        }

        for (String rawId : items.getKeys(false)) {
            ConfigurationSection section = items.getConfigurationSection(rawId);
            if (section == null) {
                continue;
            }

            ConfigurationSection fallback = resolveProfile(section, profiles, defaults);
            SummonBlockDefinition definition = loadDefinition(rawId, section, fallback, messages);
            if (definition != null) {
                result.put(definition.id(), definition);
            }
        }
        return result;
    }

    private ConfigurationSection resolveProfile(
            ConfigurationSection item, ConfigurationSection profiles, ConfigurationSection defaults) {
        String profileName = item.getString("profile", "").trim();
        if (profileName.isEmpty()) {
            return defaults;
        }
        if (profiles == null) {
            plugin.getLogger().warning("Summon profile '" + profileName + "' не найден: секция profiles отсутствует.");
            return defaults;
        }

        ConfigurationSection profile = profiles.getConfigurationSection(profileName);
        if (profile == null) {
            plugin.getLogger().warning("Summon profile '" + profileName + "' не найден. Используются defaults.");
            return defaults;
        }
        return merge(defaults, profile);
    }

    private ConfigurationSection merge(ConfigurationSection defaults, ConfigurationSection profile) {
        MemoryConfiguration merged = new MemoryConfiguration();
        if (defaults != null) {
            defaults.getValues(true).forEach((path, value) -> copyValue(merged, path, value));
        }
        profile.getValues(true).forEach((path, value) -> copyValue(merged, path, value));
        return merged;
    }

    private void copyValue(MemoryConfiguration target, String path, Object value) {
        if (value instanceof ConfigurationSection) {
            return;
        }
        target.set(path, value);
    }

    private SummonBlockDefinition loadDefinition(
            String rawId, ConfigurationSection section, ConfigurationSection fallback, SummonMessages rootMessages) {
        String id = rawId.trim().toLowerCase(Locale.ROOT);
        String eventSpec = values.string(section, fallback, "event", "").trim();
        if (eventSpec.isEmpty()) {
            plugin.getLogger().warning("Summon block '" + id + "' пропущен: не указан event.");
            return null;
        }

        ItemStack item = buildItem(id, section.getConfigurationSection("item"));
        if (item == null || item.getType().isAir() || !item.getType().isBlock()) {
            plugin.getLogger().warning("Summon block '" + id + "' пропущен: item должен быть блочным предметом.");
            return null;
        }

        String cooldownGroup =
                values.string(section, fallback, "cooldown_group", id).trim().toLowerCase(Locale.ROOT);
        if (cooldownGroup.isEmpty()) {
            cooldownGroup = id;
        }

        long cooldownSeconds = Math.max(0L, values.longValue(section, fallback, "cooldown_seconds", 0L));
        long delaySeconds = Math.max(0L, values.longValue(section, fallback, "delay_seconds", 0L));

        return new SummonBlockDefinition(
                id,
                eventSpec,
                item,
                SummonItemMatchMode.fromString(values.string(section, fallback, "match_mode", "EXACT")),
                cooldownSeconds,
                cooldownGroup,
                delaySeconds * 20L,
                SummonLocationMode.fromString(values.string(section, fallback, "location_mode", "PLACED_BLOCK")),
                values.vector(section, fallback, "location_offset", "0:0:0"),
                values.string(section, fallback, "bypass_permission", "suneventmanager.summon.bypass"),
                values.string(section, fallback, "region_bypass_permission", "suneventmanager.summon.region_bypass"),
                loadChecks(section.getConfigurationSection("checks"), values.child(fallback, "checks")),
                effectsLoader.load(section.getConfigurationSection("effects"), values.child(fallback, "effects")),
                loadMessages(section.getConfigurationSection("messages"), rootMessages));
    }

    private ItemStack buildItem(String id, ConfigurationSection itemSection) {
        if (itemSection == null) {
            plugin.getLogger().warning("Summon block '" + id + "' пропущен: секция item отсутствует.");
            return null;
        }

        try {
            ItemStack item = ItemBuilder.fromConfig(itemSection).build();
            applyGlow(item, itemSection.getBoolean("glow", false));
            return item;
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("Summon block '" + id + "': не удалось создать item: " + exception.getMessage());
            return null;
        }
    }

    private void applyGlow(ItemStack item, boolean glow) {
        if (!glow || item == null) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        Enchantment unbreaking = Enchantment.getByKey(NamespacedKey.minecraft("unbreaking"));
        if (unbreaking == null) {
            return;
        }

        meta.addEnchant(unbreaking, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
    }

    private SummonChecks loadChecks(ConfigurationSection section, ConfigurationSection fallback) {
        int minY = values.integer(section, fallback, "min_y", -64);
        int maxY = Math.max(minY, values.integer(section, fallback, "max_y", 320));

        return new SummonChecks(
                minY,
                maxY,
                values.bool(section, fallback, "require_clear_sky", false),
                values.bool(section, fallback, "require_empty_region", false),
                values.vector(section, fallback, "region_size", "15:200:15"),
                values.materials(section, fallback, "blocked_blocks"),
                values.strings(section, fallback, "allowed_worlds"),
                values.bool(section, fallback, "require_flat_surface", false),
                Math.max(0, values.integer(section, fallback, "flat_surface_radius", 1)),
                Math.max(0, values.integer(section, fallback, "maximum_surface_delta", 0)),
                Math.max(0.0D, values.doubleValue(section, fallback, "minimum_event_distance", 0.0D)));
    }

    private SummonMessages loadMessages(ConfigurationSection section, SummonMessages fallback) {
        return new SummonMessages(
                message(
                        section,
                        "success",
                        fallback == null
                                ? "&#22FF88▶&f Ивент &#22FF88{event}&f призван. &7Редкость: &f{rarity}"
                                : fallback.success()),
                message(
                        section,
                        "cooldown",
                        fallback == null
                                ? "&#FF2222▶&f Подождите &#FF2222{time}&f сек. перед следующим призывом."
                                : fallback.cooldown()),
                message(
                        section,
                        "blocked_block",
                        fallback == null
                                ? "&#FF2222▶&f На этом блоке нельзя призвать ивент."
                                : fallback.blockedBlock()),
                message(
                        section,
                        "region",
                        fallback == null ? "&#FF2222▶&f Рядом находится защищенный регион." : fallback.region()),
                message(
                        section,
                        "sky",
                        fallback == null
                                ? "&#FF2222▶&f Над точкой призыва должно быть открытое небо."
                                : fallback.sky()),
                message(
                        section,
                        "min_y",
                        fallback == null ? "&#FF2222▶&f Слишком низко для призыва ивента." : fallback.minY()),
                message(
                        section,
                        "max_y",
                        fallback == null ? "&#FF2222▶&f Слишком высоко для призыва ивента." : fallback.maxY()),
                message(
                        section,
                        "world",
                        fallback == null ? "&#FF2222▶&f В этом мире нельзя призывать данный ивент." : fallback.world()),
                message(
                        section,
                        "surface",
                        fallback == null ? "&#FF2222▶&f Для призыва нужна ровная поверхность." : fallback.surface()),
                message(
                        section,
                        "near_event",
                        fallback == null ? "&#FF2222▶&f Слишком близко находится другой ивент." : fallback.nearEvent()),
                message(
                        section,
                        "unknown_event",
                        fallback == null ? "&#FF2222▶&f Ивент &#FF2222{event}&f не найден." : fallback.unknownEvent()),
                message(
                        section,
                        "unknown_rarity",
                        fallback == null
                                ? "&#FF2222▶&f Редкость &#FF2222{rarity}&f для ивента &#FF2222{event}&f не найдена."
                                : fallback.unknownRarity()),
                message(
                        section,
                        "start_error",
                        fallback == null
                                ? "&#FF2222▶&f Не удалось призвать ивент &#FF2222{event}&f. &7{error}"
                                : fallback.startError()));
    }

    private String message(ConfigurationSection section, String path, String fallback) {
        return values.string(section, null, path, fallback);
    }

    private SummonBlockSettings disabledSettings() {
        return new SummonBlockSettings(false, true, true, loadMessages(null, null), Map.of());
    }
}
