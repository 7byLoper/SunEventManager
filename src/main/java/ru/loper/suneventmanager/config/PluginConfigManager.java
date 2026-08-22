package ru.loper.suneventmanager.config;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import ru.loper.suncore.api.config.ConfigManager;
import ru.loper.suncore.api.config.CustomConfig;
import ru.loper.suneventmanager.SunEventManager;
import ru.loper.suneventmanager.api.models.settings.CommandSettings;
import ru.loper.suneventmanager.utils.TimeUtils;

@Getter
public class PluginConfigManager extends ConfigManager {
    private boolean debug;

    private boolean voteEnabled;
    private int voteStartTime;
    private int voteDuration;
    private int minVoteEvents;
    private int maxVoteEvents;
    private int premiumVoteWeight;
    private boolean updateVoteMenu;

    private boolean eventsEnabled;
    private int eventsTime;
    private int schematicScanBlocksPerTick;

    private String errorFindLocationMessage;
    private String noPermissionMessage;
    private String eventTimeMessage;
    private String voteIsStopMessage;
    private String voteIsStartMessage;
    private List<String> voteStartMessage;
    private List<String> eventHelpMessage;
    private String eventDelayVoteMessage;
    private String eventDelayNextEventMessage;
    private String eventDelayNextVoteMessage;
    private String eventDelayNothingMessage;

    private String premiumVotePermission;

    private CommandSettings voteCommandSettings;
    private CommandSettings eventCommandSettings;
    private boolean eventDelaySubcommandEnabled;
    private NoArgumentsAction eventNoArgumentsAction;

    private LootSettingsManager lootSettingsManager;

    public PluginConfigManager(Plugin plugin) {
        super(plugin);
    }

    @Override
    public void loadConfigs() {
        plugin.saveDefaultConfig();

        addCustomConfig(new CustomConfig("schematics/README.yml", plugin));
        addCustomConfig(new CustomConfig("messages.yml", plugin));
        addCustomConfig(new CustomConfig("summon_blocks.yml", plugin));
        addCustomConfig(new CustomConfig("loot_settings.yml", plugin));
        addCustomConfig(new CustomConfig("menu/settings/events_menu.yml", plugin));
        addCustomConfig(new CustomConfig("menu/vote_menu.yml", plugin));
        addCustomConfig(new CustomConfig("menu/settings/event_settings.yml", plugin));
        addCustomConfig(new CustomConfig("menu/settings/event_rarities_menu.yml", plugin));
    }

    @Override
    public void loadValues() {
        debug = plugin.getConfig().getBoolean("debug", false);

        voteEnabled = plugin.getConfig().getBoolean("settings.vote", true);
        voteStartTime = Math.max(0, plugin.getConfig().getInt("settings.vote_start_time", 1800));
        voteDuration = Math.max(1, plugin.getConfig().getInt("settings.vote_time", 60));
        minVoteEvents = Math.max(1, plugin.getConfig().getInt("settings.vote_event_counts.min", 2));
        maxVoteEvents = Math.max(minVoteEvents, plugin.getConfig().getInt("settings.vote_event_counts.max", 4));
        premiumVoteWeight = Math.max(1, plugin.getConfig().getInt("settings.premium_vote_weight", 2));
        updateVoteMenu = plugin.getConfig().getBoolean("settings.update_vote_menu", true);
        premiumVotePermission =
                plugin.getConfig().getString("settings.premium_vote_permission", "suneventmanager.premium");

        eventCommandSettings = CommandSettings.fromSection(plugin.getConfig().getConfigurationSection("command.event"));
        voteCommandSettings = CommandSettings.fromSection(plugin.getConfig().getConfigurationSection("command.vote"));
        eventDelaySubcommandEnabled = plugin.getConfig().getBoolean("command.event.delay.enable", true);
        eventNoArgumentsAction =
                NoArgumentsAction.fromString(plugin.getConfig().getString("command.event.no_arguments", "MESSAGE"));

        eventsEnabled = plugin.getConfig().getBoolean("settings.events_spawn", true);
        eventsTime = Math.max(0, plugin.getConfig().getInt("settings.events_time", 1800));
        schematicScanBlocksPerTick =
                Math.max(256, plugin.getConfig().getInt("settings.schematic_scan_blocks_per_tick", 4096));

        CustomConfig messagesConfig = getCustomConfig("messages.yml");
        noPermissionMessage = messagesConfig.getConfig().getString("no_permission", "У вас недостаточно прав");
        errorFindLocationMessage = String.join("\n", messagesConfig.getConfig().getStringList("error_find_location"));
        eventTimeMessage = String.join("\n", messagesConfig.getConfig().getStringList("event_time_message"));
        voteIsStopMessage = messagesConfig.getConfig().getString("vote_is_stop", "Голосование еще не началось");
        voteIsStartMessage = messagesConfig.getConfig().getString("vote_is_start", "Голосование уже началось");
        voteStartMessage = List.copyOf(messagesConfig.getConfig().getStringList("vote_start_message"));
        eventHelpMessage = List.copyOf(messagesConfig.getConfig().getStringList("event_help_message"));
        eventDelayVoteMessage = messagesConfig
                .getConfig()
                .getString("event_delay.vote_active", "&#00D8FF▶&f Голосование завершится через &#00D8FF{time}&f.");
        eventDelayNextEventMessage = messagesConfig
                .getConfig()
                .getString(
                        "event_delay.next_event", "&#00D8FF▶&f Следующий автоматический ивент через &#00D8FF{time}&f.");
        eventDelayNextVoteMessage = messagesConfig
                .getConfig()
                .getString("event_delay.next_vote", "&#00D8FF▶&f Следующее голосование через &#00D8FF{time}&f.");
        eventDelayNothingMessage = messagesConfig
                .getConfig()
                .getString("event_delay.nothing", "&#FF2222▶&f Активных и запланированных ивентов нет.");

        lootSettingsManager = new LootSettingsManager(getCustomConfig("loot_settings.yml"));
    }

    public int getRandomVoteCount() {
        if (minVoteEvents == maxVoteEvents) {
            return minVoteEvents;
        }
        return ThreadLocalRandom.current().nextInt(minVoteEvents, maxVoteEvents + 1);
    }

    public String getFormattedTimeMessage(int seconds) {
        return eventTimeMessage.replace("{time}", TimeUtils.formatSeconds(Math.max(0, seconds)));
    }

    public void sendVoteStartMessage() {
        Bukkit.getOnlinePlayers()
                .forEach(player -> SunEventManager.getInstance()
                        .getMessageService()
                        .sendLines(player, voteStartMessage, Collections.emptyMap()));
    }

    public List<String> getEventHelpMessage() {
        return eventHelpMessage == null ? Collections.emptyList() : eventHelpMessage;
    }

    public String getMessage(String path, String fallback) {
        CustomConfig messagesConfig = getCustomConfig("messages.yml");
        if (messagesConfig == null || path == null || path.isBlank()) {
            return fallback;
        }

        return messagesConfig.getConfig().getString(path, fallback);
    }

    public List<String> getMessageList(String path, List<String> fallback) {
        CustomConfig messagesConfig = getCustomConfig("messages.yml");
        if (messagesConfig == null || path == null || path.isBlank()) {
            return fallback == null ? Collections.emptyList() : List.copyOf(fallback);
        }

        List<String> configured = messagesConfig.getConfig().getStringList(path);
        if (configured.isEmpty()) {
            return fallback == null ? Collections.emptyList() : List.copyOf(fallback);
        }
        return List.copyOf(configured);
    }

    public CustomConfig getEventsMenuConfig() {
        return getCustomConfig("menu/settings/events_menu.yml");
    }

    public CustomConfig getVoteMenuConfig() {
        return getCustomConfig("menu/vote_menu.yml");
    }

    public CustomConfig getEventSettingsMenuConfig() {
        return getCustomConfig("menu/settings/event_settings.yml");
    }

    public CustomConfig getEventRaritiesMenuConfig() {
        return getCustomConfig("menu/settings/event_rarities_menu.yml");
    }

    public CustomConfig getSummonBlocksConfig() {
        return getCustomConfig("summon_blocks.yml");
    }

    public enum NoArgumentsAction {
        NONE,
        MESSAGE;

        public static NoArgumentsAction fromString(String value) {
            if (value == null) {
                return MESSAGE;
            }

            try {
                return valueOf(value.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                return MESSAGE;
            }
        }
    }
}
