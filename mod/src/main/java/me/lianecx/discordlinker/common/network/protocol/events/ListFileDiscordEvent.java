package me.lianecx.discordlinker.common.network.protocol.events;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.lianecx.discordlinker.common.network.protocol.payloads.InvalidPayloadException;
import me.lianecx.discordlinker.common.network.protocol.payloads.ListFilePayload;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventResponse;
import me.lianecx.discordlinker.common.util.JsonUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class ListFileDiscordEvent implements LinkerSyncDiscordEvent<ListFilePayload> {
    @Override
    public ListFilePayload decode(Object[] objects) {
        JsonObject payload = JsonUtil.parseJsonObject(objects);
        if (payload == null) throw new InvalidPayloadException(objects);

        String folder = payload.get("folder").getAsString();
        return new ListFilePayload(folder);
    }

    @Override
    public DiscordEventResponse handle(ListFilePayload payload) {
        try {
            Path folder = Paths.get(payload.folder);

            JsonArray content = new JsonArray();
            Stream<Path> stream = Files.list(folder);
            stream.map(path -> {
                JsonObject object = new JsonObject();
                object.addProperty("name", path.toFile().getName());
                object.addProperty("isDirectory", path.toFile().isDirectory());
                return object;
            }).forEach(content::add);
            stream.close();

            return new DiscordEventResponse(content);
        }
        catch(InvalidPathException err) {
            return DiscordEventResponse.NOT_FOUND;
        }
        catch(IOException err) {
            return DiscordEventResponse.IO_ERROR;
        }
    }
}
