package me.lianecx.discordlinker.common.network.protocol.payloads;

public class ListFilePayload implements DiscordEventPayload {

    public final String folder;

    public ListFilePayload(String folder) {
        this.folder = folder;
    }
}
