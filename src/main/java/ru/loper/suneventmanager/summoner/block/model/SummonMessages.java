package ru.loper.suneventmanager.summoner.block.model;

public record SummonMessages(
        String success,
        String cooldown,
        String blockedBlock,
        String region,
        String sky,
        String minY,
        String maxY,
        String world,
        String surface,
        String nearEvent,
        String unknownEvent,
        String unknownRarity,
        String startError) {}
