package ru.loper.suneventmanager.summoner.block.model;

import java.util.Set;
import org.bukkit.Material;
import org.bukkit.util.Vector;

public record SummonChecks(
        int minY,
        int maxY,
        boolean requireClearSky,
        boolean requireEmptyRegion,
        Vector regionSize,
        Set<Material> blockedBlocks,
        Set<String> allowedWorlds,
        boolean requireFlatSurface,
        int flatSurfaceRadius,
        int maximumSurfaceDelta,
        double minimumEventDistance) {}
