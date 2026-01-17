package me.lianecx.discordlinker.common.network.protocol.payloads;

import java.io.InputStream;

public class PutFilePayload implements DiscordEventPayload {
    private final String path;
    private final InputStream stream;

    public PutFilePayload(String path, InputStream stream) {
        this.path = path;
        this.stream = stream;
    }

    public String path() {
        return path;
    }

    public InputStream stream() {
        return stream;
    }
}
