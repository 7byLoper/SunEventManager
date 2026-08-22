package ru.loper.suneventmanager.api.enums;

public enum HologramTarget {
    CHESTS,
    EVENT,
    BOTH;

    public static HologramTarget parse(String value) {
        if (value == null || value.isBlank()) {
            return CHESTS;
        }

        try {
            return valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return CHESTS;
        }
    }
}
