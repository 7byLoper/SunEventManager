package ru.loper.suneventmanager.api.models.settings;

import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import ru.loper.suncore.api.sound.SoundPlayer;

@Data
public class SoundSettings {
    private final Sound sound;
    private final float volume;
    private final float pitch;

    public static SoundSettings fromString(String soundString) {
        if (soundString == null || soundString.isEmpty()) {
            return new SoundSettings(null, 1.0f, 1.0f);
        }

        String[] parts = soundString.split(";");
        try {
            Sound sound = Sound.valueOf(parts[0]);
            float volume = parts.length > 1 ? Float.parseFloat(parts[1]) : 1.0f;
            float pitch = parts.length > 2 ? Float.parseFloat(parts[2]) : 1.0f;

            return new SoundSettings(sound, volume, pitch);
        } catch (IllegalArgumentException exception) {
            Bukkit.getLogger()
                    .warning("[SunEventManager] Некорректный звук: " + soundString
                            + ". Формат: SOUND_NAME;volume;pitch");
            return new SoundSettings(null, 1.0f, 1.0f);
        }
    }

    public void play(Player player) {
        if (sound != null) {
            SoundPlayer.play(player, sound, volume, pitch);
        }
    }
}
