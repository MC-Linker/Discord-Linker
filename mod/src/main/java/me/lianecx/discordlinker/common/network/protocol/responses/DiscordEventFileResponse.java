package me.lianecx.discordlinker.common.network.protocol.responses;

public class DiscordEventFileResponse implements DiscordEventResponse {
    private final String path;

    public DiscordEventFileResponse(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}