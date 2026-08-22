package ru.loper.suneventmanager.manager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.loper.suncore.api.config.CustomConfig;
import ru.loper.suneventmanager.SunEventManager;
import ru.loper.suneventmanager.config.EventConfigManager;
import ru.loper.suneventmanager.summoner.block.config.SummonBlockConfigLoader;
import ru.loper.suneventmanager.summoner.block.model.SummonBlockDefinition;
import ru.loper.suneventmanager.summoner.block.model.SummonBlockSettings;
import ru.loper.suneventmanager.summoner.block.model.SummonItemMatchMode;
import ru.loper.suneventmanager.summoner.block.model.SummonMessages;

@Getter
public final class SummonBlockManager {
    private final SunEventManager plugin;
    private final CustomConfig config;
    private final NamespacedKey summonIdKey;
    private final SummonBlockConfigLoader loader;
    private final Map<Plugin, Map<String, SummonBlockDefinition>> eventDefinitions = new ConcurrentHashMap<>();

    private SummonBlockSettings settings;

    public SummonBlockManager(@NotNull SunEventManager plugin, @NotNull CustomConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.summonIdKey = new NamespacedKey(plugin, "summon_id");
        this.loader = new SummonBlockConfigLoader(plugin);
        load();
    }

    public void load() {
        settings = loader.load(config);
        plugin.getLogger()
                .info("Загружено summon-блоков EventManager: "
                        + settings.definitions().size());
    }

    public void registerEventConfig(
            @NotNull Plugin owner, @NotNull String eventId, @NotNull EventConfigManager eventConfig) {
        ConfigurationSection root = eventConfig.getEventConfig().getConfig().getConfigurationSection("summoners");
        if (root == null) {
            return;
        }

        Map<String, SummonBlockDefinition> loaded =
                loader.loadExternal(root, getMessages(), owner.getName() + "/" + eventId + "/summoners");
        if (loaded.isEmpty()) {
            return;
        }

        Map<String, SummonBlockDefinition> ownerDefinitions =
                eventDefinitions.computeIfAbsent(owner, ignored -> new ConcurrentHashMap<>());

        for (Map.Entry<String, SummonBlockDefinition> entry : loaded.entrySet()) {
            String id = entry.getKey();
            SummonBlockDefinition existing = getDefinition(id);
            if (existing != null && !ownerDefinitions.containsKey(id)) {
                plugin.getLogger()
                        .warning("Summon id '" + id + "' из " + owner.getName()
                                + " пропущен: такой id уже зарегистрирован.");
                continue;
            }
            ownerDefinitions.put(id, entry.getValue());
        }

        plugin.getLogger().info("Загружено summon-блоков из " + owner.getName() + "/" + eventId + ": " + loaded.size());
    }

    public void unregisterEventConfigs(@NotNull Plugin owner) {
        eventDefinitions.remove(owner);
    }

    public @Nullable SummonBlockDefinition findDefinition(@Nullable ItemStack item) {
        if (!isEnabled() || item == null || item.getType() == Material.AIR) {
            return null;
        }

        String markedId = readMarkedId(item);
        if (markedId != null) {
            SummonBlockDefinition marked = getDefinition(markedId);
            if (marked != null && marked.item().getType() == item.getType()) {
                return marked;
            }
        }

        for (SummonBlockDefinition definition : getDefinitions().values()) {
            if (matches(definition, item)) {
                return definition;
            }
        }
        return null;
    }

    public @Nullable SummonBlockDefinition getDefinition(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }

        String normalized = id.trim().toLowerCase(Locale.ROOT);
        SummonBlockDefinition common = settings.definitions().get(normalized);
        if (common != null) {
            return common;
        }

        for (Map<String, SummonBlockDefinition> definitions : eventDefinitions.values()) {
            SummonBlockDefinition definition = definitions.get(normalized);
            if (definition != null) {
                return definition;
            }
        }
        return null;
    }

    public @Nullable ItemStack createItem(String id, int amount) {
        SummonBlockDefinition definition = getDefinition(id);
        if (definition == null) {
            return null;
        }

        ItemStack item = definition.item().clone();
        item.setAmount(Math.max(1, Math.min(item.getMaxStackSize(), amount)));

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(summonIdKey, PersistentDataType.STRING, definition.id());
            item.setItemMeta(meta);
        }
        return item;
    }

    public Map<String, SummonBlockDefinition> getDefinitions() {
        Map<String, SummonBlockDefinition> combined = new LinkedHashMap<>(settings.definitions());
        eventDefinitions.values().forEach(definitions -> definitions.forEach(combined::putIfAbsent));
        return Collections.unmodifiableMap(combined);
    }

    public Set<String> getIds() {
        return Collections.unmodifiableSet(getDefinitions().keySet());
    }

    public boolean isEnabled() {
        return settings != null && settings.enabled();
    }

    public boolean isCancelBlockPlace() {
        return settings != null && settings.cancelBlockPlace();
    }

    public boolean isConsumeItem() {
        return settings != null && settings.consumeItem();
    }

    public SummonMessages getMessages() {
        return settings.messages();
    }

    private boolean matches(SummonBlockDefinition definition, ItemStack item) {
        if (definition == null || definition.item() == null) {
            return false;
        }

        if (definition.itemMatchMode() == SummonItemMatchMode.MATERIAL) {
            return definition.item().getType() == item.getType();
        }

        return definition.item().isSimilar(item);
    }

    private String readMarkedId(ItemStack item) {
        if (!item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(summonIdKey, PersistentDataType.STRING);
    }
}
