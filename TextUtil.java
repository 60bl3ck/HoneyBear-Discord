package pl.blackcode.honeybeardiscord;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private TextUtil() {
    }

    public static Component color(String message) {
        if (message == null || message.isBlank()) {
            return Component.empty();
        }

        return LegacyComponentSerializer.legacyAmpersand().deserialize(convertHex(message));
    }

    public static Component color(String message, Map<String, String> placeholders) {
        if (message == null || message.isBlank()) {
            return Component.empty();
        }

        String replaced = message;

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            replaced = replaced.replace(entry.getKey(), entry.getValue());
        }

        return color(replaced);
    }

    public static Component clickable(String message, String hover, String url) {
        return color(message)
                .clickEvent(ClickEvent.openUrl(url))
                .hoverEvent(HoverEvent.showText(color(hover)));
    }

    private static String convertHex(String message) {
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuilder builder = new StringBuilder();

        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("&x");

            for (char character : hex.toCharArray()) {
                replacement.append('&').append(character);
            }

            matcher.appendReplacement(builder, replacement.toString());
        }

        matcher.appendTail(builder);
        return builder.toString();
    }
}