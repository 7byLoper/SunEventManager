package ru.loper.suneventmanager.summoner.block.model;

import java.util.Locale;

public enum SummonLocationMode {
    PLACED_BLOCK,
    RANDOM;

    public static SummonLocationMode fromString(String value) {
        if (value == null || value.isBlank()) {
            return PLACED_BLOCK;
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.equals("BLOCK") || normalized.equals("PLACED") || normalized.equals("FIXED")) {
            return PLACED_BLOCK;
        }

        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return PLACED_BLOCK;
        }
    }
}
