package me.lianecx.discordlinker.common.events.data;

import me.lianecx.discordlinker.common.abstraction.LinkerPlayer;

public class PlayerQuitEventData implements MinecraftEventData {

    public final LinkerPlayer player;
    public final String quitMessage;

    public PlayerQuitEventData(LinkerPlayer player, String quitMessage) {
        this.player = player;
        this.quitMessage = quitMessage;
    }
}
