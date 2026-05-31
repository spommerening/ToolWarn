package io.github.toolwarn.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Map;

public final class MessageUtil {

    private static final LegacyComponentSerializer AMPERSAND = LegacyComponentSerializer.legacyAmpersand();

    private MessageUtil() {}

    public static Component colorComponent(String message) {
        return AMPERSAND.deserialize(message);
    }

    public static String format(String template, Map<String, String> placeholders) {
        String result = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public static String toTitleCase(String input) {
        if (input == null || input.isBlank()) return input;
        String[] words = input.toLowerCase(java.util.Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                if (!sb.isEmpty()) sb.append(' ');
                sb.append(Character.toUpperCase(word.charAt(0)));
                sb.append(word.substring(1));
            }
        }
        return sb.toString();
    }
}
