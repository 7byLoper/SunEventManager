package ru.loper.suneventmanager.message;

import org.bukkit.command.CommandSender;

@FunctionalInterface
public interface PlaceholderResolver {
    PlaceholderResolver NO_OP = (viewer, input) -> input;

    String resolve(CommandSender viewer, String input);
}
