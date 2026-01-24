package me.lianecx.discordlinker.common.network.protocol.payloads;

public class CommandPayload implements DiscordEventPayload {

    public final String command;

    public CommandPayload(String command) {
        this.command = command;
    }
}
