package me.lianecx.discordlinker.common.abstraction;

public interface LinkerCommandSender {
    void sendMessage(String message);

    boolean hasPermission(String permission);

    String getName();
}
