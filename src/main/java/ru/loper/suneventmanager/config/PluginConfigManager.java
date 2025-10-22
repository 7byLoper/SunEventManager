package ru.loper.suneventmanager.config;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import ru.loper.suncore.api.config.ConfigManager;
import ru.loper.suncore.api.config.CustomConfig;
import ru.loper.suncore.utils.Colorize;
import ru.loper.suneventmanager.utils.TimeUtils;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Getter
public class PluginConfigManager extends ConfigManager {
    private boolean debug;

    private boolean voteEnabled;
    private int voteStartTime;
    private int voteDuration;
    private int minVoteEvents;
    private int maxVoteEvents;
    private int premiumVoteWeight;

    private boolean eventsEnabled;
    private int eventsTime;

    private String errorFindLocationMessage;
    private String noPermissionMessage;
    private String eventTimeMessage;
    private String voteIsStopMessage;
    private String eventCommand;
    private String voteCommand;
    private String voteIsStartMessage;
    private List<String> voteStartMessage;

    private String premiumVotePermission;

    public PluginConfigManager(Plugin plugin) {
        super(plugin);
    }

    @Override
    public void loadConfigs() {
        plugin.saveDefaultConfig();

        addCustomConfig(new CustomConfig("schematics/README.yml", plugin));
        addCustomConfig(new CustomConfig("messages.yml", plugin));
        addCustomConfig(new CustomConfig("menu/settings/events_menu.yml", plugin));
        addCustomConfig(new CustomConfig("menu/vote_menu.yml", plugin));
        addCustomConfig(new CustomConfig("menu/settings/event_settings.yml", plugin));
        addCustomConfig(new CustomConfig("menu/settings/event_rarities_menu.yml", plugin));
    }

    @Override
    public void loadValues() {
        debug = plugin.getConfig().getBoolean("debug", false);

        voteEnabled = plugin.getConfig().getBoolean("settings.vote", true);
        voteStartTime = plugin.getConfig().getInt("settings.vote_start_time", 1800);
        voteDuration = plugin.getConfig().getInt("settings.vote_time", 60);
        minVoteEvents = plugin.getConfig().getInt("settings.vote_event_counts.min", 2);
        maxVoteEvents = plugin.getConfig().getInt("settings.vote_event_counts.max", 4);
        premiumVoteWeight = plugin.getConfig().getInt("settings.premium_vote_weight", 2);
        premiumVotePermission = plugin.getConfig().getString("settings.premium_vote_permission", "sunevents.premium");
        eventCommand = plugin.getConfig().getString("command.event", "event");
        voteCommand = plugin.getConfig().getString("command.vote", "vote");

        eventsEnabled = plugin.getConfig().getBoolean("settings.events_spawn", true);
        eventsTime = plugin.getConfig().getInt("settings.events_time", 1800);

        CustomConfig messagesConfig = getCustomConfig("messages.yml");
        noPermissionMessage = messagesConfig.configMessage("no_permission", "У вас недостаточно прав");
        errorFindLocationMessage = String.join("\n", Colorize.parse(messagesConfig.getConfig().getStringList("error_find_location")));
        eventTimeMessage = String.join("\n", Colorize.parse(messagesConfig.getConfig().getStringList("event_time_message")));
        voteIsStopMessage = messagesConfig.configMessage("vote_is_stop", "Голосование еще не началось");
        voteIsStartMessage = messagesConfig.configMessage("vote_is_start", "Голосование уже началось");
        voteStartMessage = Colorize.parse(messagesConfig.getConfig().getStringList("vote_start_message"));
    }

    public int getRandomVoteCount() {
        return ThreadLocalRandom.current().nextInt(minVoteEvents, maxVoteEvents + 1);
    }

    public String getFormattedTimeMessage(int seconds) {
        return eventTimeMessage.replace("{time}", TimeUtils.formatSeconds(seconds));
    }

    public void sendVoteStartMessage() {
        Bukkit.getOnlinePlayers().forEach(player -> voteStartMessage.forEach(player::sendMessage));
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
}