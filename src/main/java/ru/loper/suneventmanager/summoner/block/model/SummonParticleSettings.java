package ru.loper.suneventmanager.summoner.block.model;

import org.bukkit.Particle;
import org.bukkit.util.Vector;

public record SummonParticleSettings(
        boolean enabled, Particle particle, int amount, double speed, Vector offset, Vector spread) {}
