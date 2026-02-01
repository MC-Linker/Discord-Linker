package me.lianecx.discordlinker.common.events.data;

public class ConsoleCommandEventData implements MinecraftEventData {
    public final String command;

    public ConsoleCommandEventData(String command) {
        this.command = command;
    }
}
