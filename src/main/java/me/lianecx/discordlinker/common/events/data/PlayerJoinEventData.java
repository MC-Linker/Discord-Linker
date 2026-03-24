package me.lianecx.discordlinker.common.events.data;

import me.lianecx.discordlinker.common.abstraction.LinkerPlayer;

public class PlayerJoinEventData implements MinecraftEventData {

    public final LinkerPlayer player;
    public final String joinMessage;
    public final boolean skipJoinRequirementCheck;

    public PlayerJoinEventData(LinkerPlayer player, String joinMessage) {
        this(player, joinMessage, false);
    }

    public PlayerJoinEventData(LinkerPlayer player, String joinMessage, boolean skipJoinRequirementCheck) {
        this.player = player;
        this.joinMessage = joinMessage;
        this.skipJoinRequirementCheck = skipJoinRequirementCheck;
    }
}
