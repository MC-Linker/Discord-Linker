package me.lianecx.discordlinker.common.abstraction;

public interface LinkerPlayer {
    String getName();

    String getUUID();

    void sendMessage(String message);

    boolean hasPermission(String permission);
}
