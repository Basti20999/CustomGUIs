package com.basti20999.customGUIs.util;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("#([A-Fa-f0-9]{6})");

    private ColorUtil() {}

    /** Translate hex (#RRGGBB) and & color codes. */
    public static String translate(String text) {
        if (text == null) return "";
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String h = matcher.group(1);
            String replacement = "&x&" + h.charAt(0) + "&" + h.charAt(1)
                    + "&" + h.charAt(2) + "&" + h.charAt(3)
                    + "&" + h.charAt(4) + "&" + h.charAt(5);
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);
        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }

    /**
     * Resolve built-in placeholders then translate colors.
     * Supported: %player%, %player_uuid%, %world%, %x%, %y%, %z%
     */
    public static String translate(String text, Player player) {
        return translate(placeholders(text, player));
    }

    /** Resolve built-in placeholders WITHOUT color translation (for command text etc.). */
    public static String placeholders(String text, Player player) {
        if (text == null) return "";
        if (player == null) return text;
        var loc = player.getLocation();
        return text
            .replace("%player%", player.getName())
            .replace("%player_uuid%", player.getUniqueId().toString())
            .replace("%world%", player.getWorld().getName())
            .replace("%x%", String.valueOf(loc.getBlockX()))
            .replace("%y%", String.valueOf(loc.getBlockY()))
            .replace("%z%", String.valueOf(loc.getBlockZ()));
    }
}
