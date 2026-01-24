package me.lianecx.discordlinker.common.network.protocol.events;

import me.lianecx.discordlinker.common.abstraction.LinkerPlayer;
import me.lianecx.discordlinker.common.network.protocol.payloads.EmptyPayload;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventJsonResponse;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventResponse;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.stream.Collectors;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getServer;

public class ListPlayersDiscordEvent implements LinkerSyncDiscordEvent<EmptyPayload> {

    @Override
    public EmptyPayload decode(Object[] objects) {
        return new EmptyPayload();
    }


    @Override
    public DiscordEventResponse handle(EmptyPayload payload) {
        List<String> onlinePlayers = getServer().getOnlinePlayers()
                .stream()
                .map(LinkerPlayer::getName)
                .collect(Collectors.toList());
        return DiscordEventJsonResponse.toJson(onlinePlayers);
    }
}
