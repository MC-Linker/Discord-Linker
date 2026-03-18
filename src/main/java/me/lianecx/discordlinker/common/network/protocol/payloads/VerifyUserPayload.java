package me.lianecx.discordlinker.common.network.protocol.payloads;

public class VerifyUserPayload implements DiscordEventPayload {

    public final String uuid;
    public final String code;

    public VerifyUserPayload(String uuid, String code) {
        this.uuid = uuid;
        this.code = code;
    }
}
