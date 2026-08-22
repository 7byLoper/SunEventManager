package ru.loper.suneventmanager.summoner.block.model;

import java.util.Locale;

public enum SummonItemMatchMode {
    EXACT,
    MATERIAL;

    public static SummonItemMatchMode fromString(String value) {
        if (value == null || value.isBlank()) {
            return EXACT;
        }

        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return EXACT;
        }
    }
}
