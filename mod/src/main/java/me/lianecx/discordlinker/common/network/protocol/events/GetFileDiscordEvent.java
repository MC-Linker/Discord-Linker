package me.lianecx.discordlinker.common.network.protocol.events;

import com.google.gson.JsonObject;
import me.lianecx.discordlinker.common.network.protocol.payloads.GetFilePayload;
import me.lianecx.discordlinker.common.network.protocol.payloads.InvalidPayloadException;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventFileResponse;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventJsonResponse;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventResponse;
import me.lianecx.discordlinker.common.util.JsonUtil;

import java.io.File;

import static me.lianecx.discordlinker.common.util.URLEncoderUtil.decodeURL;

public class GetFileDiscordEvent implements LinkerSyncDiscordEvent<GetFilePayload> {

    @Override
    public GetFilePayload decode(Object[] objects) throws InvalidPayloadException {
        JsonObject jsonObject = JsonUtil.parseJsonObject(objects);
        if(jsonObject == null || !jsonObject.has("path")) throw new InvalidPayloadException(objects);
        String path = decodeURL(jsonObject.get("path").getAsString());
        return new GetFilePayload(path);
    }

    @Override
    public DiscordEventResponse handle(GetFilePayload payload) {
        File file = new File(payload.path);
        if(!file.isFile()) return DiscordEventJsonResponse.INVALID_PATH;

        return new DiscordEventFileResponse(file.toString());
    }
}
