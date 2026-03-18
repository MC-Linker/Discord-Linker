package me.lianecx.discordlinker.common.events.data;

import me.lianecx.discordlinker.common.abstraction.LinkerPlayer;

public class ChatEventData implements MinecraftEventData {

    public final String message;
    public final LinkerPlayer player;

    public ChatEventData(String message, LinkerPlayer player) {
        this.message = message;
        this.player = player;
    }
}
