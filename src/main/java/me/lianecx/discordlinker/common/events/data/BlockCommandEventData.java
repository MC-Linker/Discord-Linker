package me.lianecx.discordlinker.common.events.data;

public class BlockCommandEventData implements MinecraftEventData {

    public final String command;

    public BlockCommandEventData(String command) {
        this.command = command;
    }
}
