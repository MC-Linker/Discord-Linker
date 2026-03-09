package me.lianecx.discordlinker.common.network.protocol.events;

import com.google.gson.JsonObject;
import me.lianecx.discordlinker.common.network.protocol.payloads.CommandPayload;
import me.lianecx.discordlinker.common.network.protocol.payloads.InvalidPayloadException;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventResponse;
import me.lianecx.discordlinker.common.util.JsonUtil;

import java.util.concurrent.CompletableFuture;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getServer;
import static me.lianecx.discordlinker.common.util.URLEncoderUtil.decodeURL;

public class CommandCompletionsDiscordEvent implements LinkerDiscordEvent<CommandPayload> {

    @Override
    public CommandPayload decode(Object[] objects) {
        JsonObject payload = JsonUtil.parseJsonObject(objects);
        if(payload == null || !payload.has("cmd")) throw new InvalidPayloadException(objects);

        String command = decodeURL(payload.get("cmd").getAsString().trim());
        return new CommandPayload(command);
    }

    @Override
    public CompletableFuture<DiscordEventResponse> handleAsync(CommandPayload payload) {
        return getServer().getCommandCompletions(payload.command, 25)
                .thenApply(DiscordEventResponse::toJson)
                .exceptionally(ignored -> DiscordEventResponse.UNKNOWN);
    }
}