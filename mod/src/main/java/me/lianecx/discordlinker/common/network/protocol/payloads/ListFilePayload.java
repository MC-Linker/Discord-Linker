package me.lianecx.discordlinker.common.network.protocol.payloads;

public class ListFilePayload implements DiscordEventPayload {

    public final String directory;

    public ListFilePayload(String directory) {
        this.directory = directory;
    }
}
