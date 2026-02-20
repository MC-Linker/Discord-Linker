package me.lianecx.discordlinker.common.abstraction;

public interface LinkerCommandSender {
    void sendMessage(String message);

    /**
     * Checks if the sender has the specified permission.
     * If there is no permission system, the defaultLevel will be used to determine if the sender has permission.
     * @param defaultLevel The default permission level from 0-4.
     */
    boolean hasPermission(int defaultLevel, String permission);

    String getName();
}
