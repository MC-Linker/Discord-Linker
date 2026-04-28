package me.lianecx.discordlinker.common.network.protocol.events;

import com.google.gson.JsonObject;
import me.lianecx.discordlinker.common.network.protocol.payloads.GetFilePayload;
import me.lianecx.discordlinker.common.network.protocol.payloads.InvalidPayloadException;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventResponse;
import me.lianecx.discordlinker.common.util.JsonUtil;

import java.io.File;

public class GetFileDiscordEvent implements LinkerDirectSyncDiscordEvent<GetFilePayload> {

    @Override
    public GetFilePayload decode(Object[] objects) throws InvalidPayloadException {
        JsonObject jsonObject = JsonUtil.parseJsonObject(objects);
        if(jsonObject == null || !jsonObject.has("path")) throw new InvalidPayloadException(objects);
        String path = jsonObject.get("path").getAsString();
        return new GetFilePayload(path);
    }

    @Override
    public DiscordEventResponse handle(GetFilePayload payload) {
        File file = new File(payload.path);
        if(!file.isFile()) return DiscordEventResponse.NOT_FOUND;

        return DiscordEventResponse.fromFile(file.toString());
    }
}
