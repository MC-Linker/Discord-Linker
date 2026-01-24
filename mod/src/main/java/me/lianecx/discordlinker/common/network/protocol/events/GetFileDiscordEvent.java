package me.lianecx.discordlinker.common.network.protocol.events;

import com.google.gson.JsonObject;
import me.lianecx.discordlinker.common.network.protocol.payloads.GetFilePayload;
import me.lianecx.discordlinker.common.network.protocol.payloads.InvalidPayloadException;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventFileResponse;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventJsonResponse;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventResponse;
import me.lianecx.discordlinker.common.util.JsonUtil;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class GetFileDiscordEvent implements LinkerSyncDiscordEvent<GetFilePayload> {

    @Override
    public GetFilePayload decode(Object[] objects) throws InvalidPayloadException {
        JsonObject jsonObject = JsonUtil.getJsonObjectFromObjects(objects);
        if(jsonObject == null || !jsonObject.has("path")) throw new InvalidPayloadException(objects);
        try {
            String path = URLDecoder.decode(jsonObject.get("path").getAsString(), "utf-8");
            return new GetFilePayload(path);
        }
        catch(UnsupportedEncodingException e) {
            throw new InvalidPayloadException(objects);
        }
    }

    @Override
    public DiscordEventResponse handle(GetFilePayload payload) {
        File file = new File(payload.path);
        if(!file.isFile())
            return DiscordEventJsonResponse.INVALID_PATH;

        return new DiscordEventFileResponse(file.toString());
    }
}
