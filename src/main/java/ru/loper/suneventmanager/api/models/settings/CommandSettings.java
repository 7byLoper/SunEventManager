package ru.loper.suneventmanager.api.models.settings;

import lombok.Data;
import org.bukkit.configuration.ConfigurationSection;

@Data
public class CommandSettings {
    private final boolean enable;
    private final String command;
    private final boolean standalone;
    private final boolean eventSubcommand;

    public CommandSettings(boolean enable, String command, boolean standalone, boolean eventSubcommand) {
        this.enable = enable;
        this.command = command;
        this.standalone = standalone;
        this.eventSubcommand = eventSubcommand;
    }

    public CommandSettings(boolean enable, String command) {
        this(enable, command, true, false);
    }

    public static CommandSettings fromSection(ConfigurationSection section) {
        return fromSection(section, "");
    }

    public static CommandSettings fromSection(ConfigurationSection section, String defaultCommand) {
        if (section == null) {
            return new CommandSettings(false, defaultCommand, false, false);
        }

        return new CommandSettings(
                section.getBoolean("enable", true),
                section.getString("command", defaultCommand).trim(),
                section.getBoolean("standalone", true),
                section.getBoolean("event_subcommand", false));
    }
}
