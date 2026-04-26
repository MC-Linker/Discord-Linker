package me.lianecx.discordlinker.common.network.protocol.events;

import com.google.gson.JsonObject;
import me.lianecx.discordlinker.common.abstraction.LinkerPlayer;
import me.lianecx.discordlinker.common.network.protocol.payloads.GetPlayerNBTPayload;
import me.lianecx.discordlinker.common.network.protocol.payloads.InvalidPayloadException;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventResponse;
import me.lianecx.discordlinker.common.util.JsonUtil;

import java.util.UUID;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getServer;

public class GetPlayerNBTDiscordEvent implements LinkerScheduledSyncDiscordEvent<GetPlayerNBTPayload> {

    @Override
    public GetPlayerNBTPayload decode(Object[] objects) {
        JsonObject payload = JsonUtil.parseJsonObject(objects);
        if(payload == null) throw new InvalidPayloadException(objects);

        String uuid = payload.get("uuid").getAsString();
        return new GetPlayerNBTPayload(uuid);
    }

    @Override
    public DiscordEventResponse handle(GetPlayerNBTPayload payload) {
        LinkerPlayer player = getServer().getPlayer(UUID.fromString(payload.uuid));
        if(player == null) return DiscordEventResponse.PLAYER_NOT_ONLINE;

        String nbt = player.getNBTAsString();
        if(nbt == null) return DiscordEventResponse.NBT_ERROR;

        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("data", nbt);

        return new DiscordEventResponse(responseJson);
    }

}
