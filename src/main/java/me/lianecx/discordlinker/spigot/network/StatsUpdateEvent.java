package me.lianecx.discordlinker.spigot.network;

public enum StatsUpdateEvent {

    ONLINE("status"),
    OFFLINE("status"),
    MEMBERS("member-counter");

    private final String jsonKey;

    StatsUpdateEvent(String jsonKey) {
        this.jsonKey = jsonKey;
    }

    public String getName() {
        return name().toLowerCase();
    }

    public String getJsonKey() {
        return this.jsonKey;
    }
}
