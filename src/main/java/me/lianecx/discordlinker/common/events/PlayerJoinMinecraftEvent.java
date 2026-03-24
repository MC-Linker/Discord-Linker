package me.lianecx.discordlinker.common.events;

import me.lianecx.discordlinker.common.abstraction.LinkerPlayer;
import me.lianecx.discordlinker.common.events.data.PlayerJoinEventData;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.*;

public class PlayerJoinMinecraftEvent implements LinkerMinecraftEvent<PlayerJoinEventData> {

    public void handle(PlayerJoinEventData event) {
        if(event.skipJoinRequirementCheck) return;

        LinkerPlayer player = event.player;

        JoinRequirementEvaluator.evaluate(player.getUUID(), player.getName(), result -> {
            if(!result.isAllowed()) kickPlayerSynchronized(player, result.getDenyReason());
        });
    }

    public void kickPlayerSynchronized(LinkerPlayer player, String reason) {
        getScheduler().runDelayedSync(() -> {
            if(!player.isOnline()) return;
            player.kick(reason);
        }, 0);
    }
}
