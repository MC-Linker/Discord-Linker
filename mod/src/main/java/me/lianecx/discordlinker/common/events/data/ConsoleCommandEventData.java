package me.lianecx.discordlinker.common.events.data;

import me.lianecx.discordlinker.common.abstraction.LinkerCommandSender;

public class ConsoleCommandEventData implements MinecraftEventData {
    public final String command;
    public final String senderName;

    public ConsoleCommandEventData(String command, String senderName) {
        this.command = command;
        this.senderName = senderName;
    }
}
