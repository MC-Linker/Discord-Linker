package me.lianecx.discordlinker.common.util;

import com.google.gson.JsonObject;

public enum MinecraftChatColor {
    BLACK('0'),
    DARK_BLUE('1'),
    DARK_GREEN('2'),
    DARK_AQUA('3'),
    DARK_RED('4'),
    DARK_PURPLE('5'),
    GOLD('6'),
    GRAY('7'),
    DARK_GRAY('8'),
    BLUE('9'),
    GREEN('a'),
    AQUA('b'),
    RED('c'),
    LIGHT_PURPLE('d'),
    YELLOW('e'),
    WHITE('f'),
    RESET('r'),
    BOLD('l'),
    ITALIC('o'),
    UNDERLINE('n'),
    STRIKETHROUGH('m'),
    MAGIC('k');

    private final char code;

    MinecraftChatColor(char code) {
        this.code = code;
    }

    public static String replaceColorKey(String response, char c) {
        return response.replaceAll("§", String.valueOf(c));
    }

    @Override
    public String toString() {
        return "§" + code;
    }

    /**
     * Strips Minecraft color codes starting with '&' or '§' from the input string.
     */
    public static String stripColorCodes(String input) {
        return input.replaceAll("(?i)[&§][0-9A-FK-OR]",  "");
    }
}
