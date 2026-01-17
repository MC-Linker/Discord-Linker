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

public class GetFileDiscordEvent implements DiscordEvent<GetFilePayload> {

    @Override
    public GetFilePayload decode(Object[] objects) throws InvalidPayloadException {
        JsonObject jsonObject = JsonUtil.getJsonObjectFromObjects(objects);
        if(jsonObject == null || !jsonObject.has("path")) throw new InvalidPayloadException(objects);
        String path = jsonObject.get("path").getAsString();
        return new GetFilePayload(path);
    }

    @Override
    public DiscordEventResponse handle(GetFilePayload payload) {
        try {
            File file = new File(URLDecoder.decode(payload.path(), "utf-8"));
            if(!file.isFile())
                return DiscordEventJsonResponse.INVALID_PATH;

            return new DiscordEventFileResponse(file.toString());
        }
        catch(UnsupportedEncodingException err) {
            return new DiscordEventJsonResponse(DiscordEventJsonResponse.JsonStatus.ERROR, err.toString());
        }
    }
}
