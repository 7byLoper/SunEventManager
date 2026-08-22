package ru.loper.suneventmanager.summoner.block.model;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public record SummonBlockDefinition(
        String id,
        String eventSpec,
        ItemStack item,
        SummonItemMatchMode itemMatchMode,
        long cooldownSeconds,
        String cooldownGroup,
        long delayTicks,
        SummonLocationMode locationMode,
        Vector locationOffset,
        String bypassPermission,
        String regionBypassPermission,
        SummonChecks checks,
        SummonEffects effects,
        SummonMessages messages) {}
