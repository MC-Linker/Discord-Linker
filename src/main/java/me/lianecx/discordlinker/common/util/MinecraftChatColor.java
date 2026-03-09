package me.lianecx.discordlinker.common.util;

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

    /**
     * Replaces all Minecraft color codes in the input string with the specified character followed by the color code.
     */
    public static String replaceColorKey(String response, char c) {
        return response.replaceAll("(?i)§([0-9A-FK-OR])", c + "$1");
    }

    /**
     * Translates a string using an alternate color code character into a string that uses the internal Minecraft color code character (§).
     */
    public static String translateAlternateColorCodes(String textToTranslate, char altColorChar) {
        return textToTranslate.replaceAll("(?i)" + altColorChar + "([0-9A-FK-OR])", "§$1");
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
