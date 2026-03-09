package me.lianecx.discordlinker.common.network.protocol.payloads;

public class GetPlayerNBTPayload implements DiscordEventPayload {
    public final String uuid;

    public GetPlayerNBTPayload(String uuid) {
        this.uuid = uuid;
    }
}
