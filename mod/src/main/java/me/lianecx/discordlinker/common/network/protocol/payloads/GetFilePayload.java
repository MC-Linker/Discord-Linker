package me.lianecx.discordlinker.common.network.protocol.payloads;

public class GetFilePayload implements DiscordEventPayload {
    private final String path;

    public GetFilePayload(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }
}
