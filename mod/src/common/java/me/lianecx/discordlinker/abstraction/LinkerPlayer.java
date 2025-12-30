package me.lianecx.discordlinker.abstraction;

public interface LinkerPlayer {
    String getName();

    String getUUID();

    void sendMessage(String message);

    boolean hasPermission(String permission);
}
