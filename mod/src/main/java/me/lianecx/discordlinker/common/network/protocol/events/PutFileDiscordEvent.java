package me.lianecx.discordlinker.common.network.protocol.events;

import com.google.gson.JsonObject;
import me.lianecx.discordlinker.common.network.protocol.payloads.InvalidPayloadException;
import me.lianecx.discordlinker.common.network.protocol.payloads.PutFilePayload;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventJsonResponse;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventResponse;
import me.lianecx.discordlinker.common.util.JsonUtil;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class PutFileDiscordEvent implements LinkerSyncDiscordEvent<PutFilePayload> {

    @Override
    public PutFilePayload decode(Object[] objects) throws InvalidPayloadException {
        JsonObject payload = JsonUtil.getJsonObjectFromObjects(objects);
        if(payload == null || objects.length < 2) throw new InvalidPayloadException(objects);
        if(!(objects[1] instanceof InputStream))
            throw new InvalidPayloadException(objects);

        String path;
        try {
            path = URLDecoder.decode(payload.get("path").getAsString(), "utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new InvalidPayloadException(objects);
        }
        InputStream fileData = (InputStream) objects[1];
        return new PutFilePayload(path, fileData);
    }

    @Override
    public DiscordEventResponse handle(PutFilePayload payload) {
        try(FileOutputStream outputStream = new FileOutputStream(payload.path)) {
            //Transfer body (inputStream) to outputStream
            byte[] buf = new byte[8192];
            int length;
            while((length = payload.stream.read(buf)) > 0) {
                outputStream.write(buf, 0, length);
            }

            return DiscordEventJsonResponse.SUCCESS;
        }
        catch(IOException err) {
            JsonObject error = new JsonObject();
            error.addProperty("message", err.toString());
            return new DiscordEventJsonResponse(error);
        }
    }

}
