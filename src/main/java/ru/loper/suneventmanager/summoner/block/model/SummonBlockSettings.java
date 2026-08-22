package ru.loper.suneventmanager.summoner.block.model;

import java.util.Map;

public record SummonBlockSettings(
        boolean enabled,
        boolean cancelBlockPlace,
        boolean consumeItem,
        SummonMessages messages,
        Map<String, SummonBlockDefinition> definitions) {}
