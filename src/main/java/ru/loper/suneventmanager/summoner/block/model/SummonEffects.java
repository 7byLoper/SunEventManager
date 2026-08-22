package ru.loper.suneventmanager.summoner.block.model;

import java.util.List;

public record SummonEffects(
        SummonSoundSettings successSound,
        SummonSoundSettings failedSound,
        SummonSoundSettings spawnSound,
        List<SummonParticleSettings> particles) {}
