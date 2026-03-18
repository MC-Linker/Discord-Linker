package me.lianecx.discordlinker.common.events.data;

import me.lianecx.discordlinker.common.abstraction.LinkerPlayer;

public class PlayerDeathEventData implements MinecraftEventData {

    public final LinkerPlayer player;
    public final String deathMessage;

    public PlayerDeathEventData(LinkerPlayer player, String deathMessage) {
        this.player = player;
        this.deathMessage = deathMessage;
    }
}
