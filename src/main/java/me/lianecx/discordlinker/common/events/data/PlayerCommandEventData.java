package me.lianecx.discordlinker.common.events.data;

import me.lianecx.discordlinker.common.abstraction.LinkerPlayer;

public class PlayerCommandEventData implements MinecraftEventData {
    public final String command;
    public final LinkerPlayer player;

    public PlayerCommandEventData(String command, LinkerPlayer player) {
        this.command = command;
        this.player = player;
    }
}
