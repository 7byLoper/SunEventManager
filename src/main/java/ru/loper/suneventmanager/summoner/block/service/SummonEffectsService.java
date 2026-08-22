package ru.loper.suneventmanager.summoner.block.service;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import ru.loper.suncore.api.sound.SoundPlayer;
import ru.loper.suneventmanager.SunEventManager;
import ru.loper.suneventmanager.summoner.block.model.SummonBlockDefinition;
import ru.loper.suneventmanager.summoner.block.model.SummonParticleSettings;
import ru.loper.suneventmanager.summoner.block.model.SummonSoundSettings;

public final class SummonEffectsService {

    public void playSuccess(Player player, Location location, SummonBlockDefinition definition) {
        spawnParticles(location, definition);
        playSound(player, definition.effects().successSound());
    }

    public void playFailed(Player player, SummonBlockDefinition definition) {
        playSound(player, definition.effects().failedSound());
    }

    public void playSpawn(Player player, SummonBlockDefinition definition) {
        playSound(player, definition.effects().spawnSound());
    }

    private void spawnParticles(Location location, SummonBlockDefinition definition) {
        if (location == null || location.getWorld() == null) {
            return;
        }

        World world = location.getWorld();
        for (SummonParticleSettings settings : definition.effects().particles()) {
            if (!settings.enabled() || settings.particle() == null) {
                continue;
            }

            Vector offset = settings.offset();
            Vector spread = settings.spread();
            Location effectLocation = location.clone().add(offset);
            try {
                world.spawnParticle(
                        settings.particle(),
                        effectLocation,
                        settings.amount(),
                        spread.getX(),
                        spread.getY(),
                        spread.getZ(),
                        settings.speed());
            } catch (RuntimeException exception) {
                SunEventManager.getInstance()
                        .getLogger()
                        .warning("Не удалось отобразить summon particle '" + settings.particle() + "': "
                                + exception.getMessage());
            }
        }
    }

    private void playSound(Player source, SummonSoundSettings settings) {
        if (settings == null || !settings.enabled() || settings.sound() == null) {
            return;
        }

        if (settings.global()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                SoundPlayer.play(player, settings.sound(), settings.volume(), settings.pitch());
            }
            return;
        }

        if (source != null) {
            SoundPlayer.play(source, settings.sound(), settings.volume(), settings.pitch());
        }
    }
}
