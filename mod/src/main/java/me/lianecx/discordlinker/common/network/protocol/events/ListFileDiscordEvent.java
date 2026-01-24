package me.lianecx.discordlinker.common.network.protocol.events;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.lianecx.discordlinker.common.network.protocol.payloads.InvalidPayloadException;
import me.lianecx.discordlinker.common.network.protocol.payloads.ListFilePayload;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventJsonResponse;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class ListFileDiscordEvent implements LinkerSyncDiscordEvent<ListFilePayload> {
    @Override
    public ListFilePayload decode(Object[] objects) {
        if (objects.length != 1) throw new InvalidPayloadException(objects);

        String directory = (String) objects[0];
        return new ListFilePayload(directory);
    }

    @Override
    public DiscordEventJsonResponse handle(ListFilePayload payload) {
        try {
            Path folder = Paths.get(URLDecoder.decode(payload.directory, "utf-8"));

            JsonArray content = new JsonArray();
            Stream<Path> stream = Files.list(folder);
            stream.map(path -> {
                JsonObject object = new JsonObject();
                object.addProperty("name", path.toFile().getName());
                object.addProperty("isDirectory", path.toFile().isDirectory());
                return object;
            }).forEach(content::add);
            stream.close();

            return new DiscordEventJsonResponse(DiscordEventJsonResponse.JsonStatus.SUCCESS, content);
        }
        catch(InvalidPathException err) {
            return DiscordEventJsonResponse.INVALID_PATH;
        }
        catch(IOException err) {
            return DiscordEventJsonResponse.IO_ERROR;
        }
    }
}
