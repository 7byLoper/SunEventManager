package ru.loper.suneventmanager.message;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class LegacyMiniMessageAdapter {
    private static final Pattern REPEATED_HEX = Pattern.compile("(?i)[&§]x(?:[&§][0-9a-f]){6}");
    private static final Pattern SIMPLE_HEX = Pattern.compile("(?i)[&§]#([0-9a-f]{6})");
    private static final Pattern LEGACY_CODE = Pattern.compile("(?i)[&§]([0-9a-fk-or])");

    private static final Map<Character, String> COLORS = Map.ofEntries(
            Map.entry('0', "black"),
            Map.entry('1', "dark_blue"),
            Map.entry('2', "dark_green"),
            Map.entry('3', "dark_aqua"),
            Map.entry('4', "dark_red"),
            Map.entry('5', "dark_purple"),
            Map.entry('6', "gold"),
            Map.entry('7', "gray"),
            Map.entry('8', "dark_gray"),
            Map.entry('9', "blue"),
            Map.entry('a', "green"),
            Map.entry('b', "aqua"),
            Map.entry('c', "red"),
            Map.entry('d', "light_purple"),
            Map.entry('e', "yellow"),
            Map.entry('f', "white"));

    private LegacyMiniMessageAdapter() {}

    static String convert(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        String result = convertRepeatedHex(input);
        result = convertSimpleHex(result);
        return convertLegacyCodes(result);
    }

    private static String convertRepeatedHex(String input) {
        Matcher matcher = REPEATED_HEX.matcher(input);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String value = matcher.group();
            StringBuilder hex = new StringBuilder(6);
            for (int index = 3; index < value.length(); index += 2) {
                hex.append(value.charAt(index));
            }

            matcher.appendReplacement(buffer, Matcher.quoteReplacement("<reset><#" + hex + ">"));
        }

        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String convertSimpleHex(String input) {
        Matcher matcher = SIMPLE_HEX.matcher(input);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            matcher.appendReplacement(buffer, Matcher.quoteReplacement("<reset><#" + matcher.group(1) + ">"));
        }

        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String convertLegacyCodes(String input) {
        Matcher matcher = LEGACY_CODE.matcher(input);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            char code = matcher.group(1).toLowerCase(Locale.ROOT).charAt(0);
            String replacement = replacement(code);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String replacement(char code) {
        String color = COLORS.get(code);
        if (color != null) {
            return "<reset><" + color + ">";
        }

        return switch (code) {
            case 'k' -> "<obfuscated>";
            case 'l' -> "<bold>";
            case 'm' -> "<strikethrough>";
            case 'n' -> "<underlined>";
            case 'o' -> "<italic>";
            case 'r' -> "<reset>";
            default -> "";
        };
    }
}
