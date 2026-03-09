package me.lianecx.discordlinker.common.network.protocol.payloads;

public class GetFilePayload implements DiscordEventPayload {
    public final String path;

    public GetFilePayload(String path) {
        this.path = path;
    }
}
