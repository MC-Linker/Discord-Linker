package me.lianecx.discordlinker.common.network.protocol.events;

import com.google.gson.JsonObject;
import me.lianecx.discordlinker.common.abstraction.LinkerPlayer;
import me.lianecx.discordlinker.common.network.protocol.payloads.GetPlayerNBTPayload;
import me.lianecx.discordlinker.common.network.protocol.payloads.InvalidPayloadException;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventJsonResponse;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventResponse;

import java.util.UUID;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getServer;

public class GetPlayerNBTDiscordEvent implements LinkerSyncDiscordEvent<GetPlayerNBTPayload> {

    @Override
    public GetPlayerNBTPayload decode(Object[] objects) {
        if (objects.length != 1) throw new InvalidPayloadException(objects);

        String uuid = (String) objects[0];
        return new GetPlayerNBTPayload(uuid);
    }

    @Override
    public DiscordEventResponse handle(GetPlayerNBTPayload payload) {
        LinkerPlayer player = getServer().getPlayer(UUID.fromString(payload.uuid));
        if(player == null) return DiscordEventJsonResponse.INVALID_PLAYER;

        String nbt = player.getNBTAsString();
        if(nbt == null) return DiscordEventJsonResponse.NBT_ERROR;

        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("data", nbt);

        return new DiscordEventJsonResponse(DiscordEventJsonResponse.JsonStatus.SUCCESS, responseJson);
    }

}
