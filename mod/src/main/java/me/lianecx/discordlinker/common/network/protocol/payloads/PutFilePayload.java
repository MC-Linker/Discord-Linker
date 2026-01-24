package me.lianecx.discordlinker.common.network.protocol.payloads;

import java.io.InputStream;

public class PutFilePayload implements DiscordEventPayload {
    public final String path;
    public final InputStream stream;

    public PutFilePayload(String path, InputStream stream) {
        this.path = path;
        this.stream = stream;
    }
}
