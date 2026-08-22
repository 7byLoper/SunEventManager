package ru.loper.suneventmanager.summoner.block.model;

import org.bukkit.Sound;

public record SummonSoundSettings(boolean enabled, Sound sound, float volume, float pitch, boolean global) {}
