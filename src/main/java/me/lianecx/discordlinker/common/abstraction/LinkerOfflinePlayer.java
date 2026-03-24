package me.lianecx.discordlinker.common.abstraction;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

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

    public static String offlineUuid(String username) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8)).toString();
    }
}
