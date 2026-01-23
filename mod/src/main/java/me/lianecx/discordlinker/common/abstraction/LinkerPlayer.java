package me.lianecx.discordlinker.common.abstraction;

public abstract class LinkerPlayer extends LinkerOfflinePlayer {

    public LinkerPlayer(String uuid, String name) {
        super(uuid, name);
    }

    abstract public void sendMessage(String message);

    abstract public boolean hasPermission(String permission);

    abstract public void kick(String reason);

    public boolean isOnline() {
        return true;
    }
}
