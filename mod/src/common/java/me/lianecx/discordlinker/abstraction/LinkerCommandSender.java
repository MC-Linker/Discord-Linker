package me.lianecx.discordlinker.abstraction;

public interface LinkerCommandSender {
    void sendMessage(String message);

    boolean hasPermission(String permission);

    String getName();
}
