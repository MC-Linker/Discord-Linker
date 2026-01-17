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

    @Override
    public String toString() {
        return "§" + code;
    }
}
