package me.lianecx.discordlinker.common.events.data;

import me.lianecx.discordlinker.common.abstraction.LinkerPlayer;

public class AdvancementEventData implements MinecraftEventData {

    public final LinkerPlayer player;
    public final String advancementKey;

    public AdvancementEventData(LinkerPlayer player, String advancementKey) {
        this.player = player;
        this.advancementKey = advancementKey;
    }
}
