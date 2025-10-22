package ru.loper.suneventmanager.utils;

import lombok.Getter;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

@Getter
public class SoundSettings {
    private final Sound sound;
    private final float volume;
    private final float pitch;

    public SoundSettings(String soundString) {
        if (soundString == null || soundString.isEmpty()) {
            this.sound = null;
            this.volume = 1.0f;
            this.pitch = 1.0f;
            return;
        }

        String[] parts = soundString.split(";");
        try {
            this.sound = Sound.valueOf(parts[0]);
            this.volume = parts.length > 1 ? Float.parseFloat(parts[1]) : 1.0f;
            this.pitch = parts.length > 2 ? Float.parseFloat(parts[2]) : 1.0f;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid sound format: " + soundString + ". Expected format: SOUND_NAME;volume;pitch", e);
        }
    }

    public void play(Player player) {
        if (sound != null) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }
}