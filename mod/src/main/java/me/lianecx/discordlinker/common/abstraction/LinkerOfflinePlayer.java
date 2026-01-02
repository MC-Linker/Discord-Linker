package me.lianecx.discordlinker.common.abstraction;

public class LinkerOfflinePlayer {

    String uuid;
    String name;

    public LinkerOfflinePlayer(String uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getUUID() {
        return uuid;
    }

    public boolean isOnline() {
        return false;
    }
}
