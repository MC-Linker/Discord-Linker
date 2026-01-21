package me.lianecx.discordlinker.common.network.protocol.events;

import com.google.gson.JsonObject;
import me.lianecx.discordlinker.common.network.protocol.payloads.InvalidPayloadException;
import me.lianecx.discordlinker.common.network.protocol.payloads.PutFilePayload;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventJsonResponse;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventResponse;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;

public class PutFileDiscordEvent implements LinkerDiscordEvent<PutFilePayload> {

    @Override
    public PutFilePayload decode(Object[] objects) throws InvalidPayloadException {
        if(objects.length < 2)
            throw new InvalidPayloadException(objects);
        if(!(objects[0] instanceof String) || !(objects[1] instanceof InputStream))
            throw new InvalidPayloadException(objects);

        String path = (String) objects[0];
        InputStream fileData = (InputStream) objects[1];
        return new PutFilePayload(path, fileData);
    }

    @Override
    public DiscordEventResponse handle(PutFilePayload payload) {
        try(FileOutputStream outputStream = new FileOutputStream(URLDecoder.decode(payload.path(), "utf-8"))) {
            //Transfer body (inputStream) to outputStream
            byte[] buf = new byte[8192];
            int length;
            while((length = payload.stream().read(buf)) > 0) {
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
