package me.lianecx.discordlinker.spigot.network;

public enum ChatType {
    CHAT,
    JOIN,
    QUIT,
    ADVANCEMENT,
    DEATH,
    PLAYER_COMMAND,
    CONSOLE_COMMAND,
    BLOCK_COMMAND,
    START,
    CLOSE;

    public String getKey() {
        return name().toLowerCase();
    }
}
