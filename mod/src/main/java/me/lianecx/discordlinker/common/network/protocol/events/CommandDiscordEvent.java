package me.lianecx.discordlinker.common.network.protocol.events;

import com.google.gson.JsonObject;
import me.lianecx.discordlinker.common.network.protocol.payloads.CommandPayload;
import me.lianecx.discordlinker.common.network.protocol.payloads.InvalidPayloadException;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventResponse;
import me.lianecx.discordlinker.common.util.JsonUtil;
import me.lianecx.discordlinker.common.util.MinecraftChatColor;

import java.util.concurrent.CompletableFuture;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.*;
import static me.lianecx.discordlinker.common.util.URLEncoderUtil.decodeURL;

public class CommandDiscordEvent implements LinkerDiscordEvent<CommandPayload> {

    @Override
    public CommandPayload decode(Object[] objects) {
        JsonObject payload = JsonUtil.parseJsonObject(objects);
        if(payload == null) throw new InvalidPayloadException(objects);

        String command = decodeURL(payload.get("cmd").getAsString().trim());
        return new CommandPayload(command);
    }

    @Override
    public CompletableFuture<DiscordEventResponse> handleAsync(CommandPayload payload) {
        CompletableFuture<DiscordEventResponse> future = new CompletableFuture<>();
        getScheduler().runDelayedSync(() -> {
            getLogger().info(MinecraftChatColor.AQUA + "Command from Discord: /" + payload.command);
            getServer().executeCommand(payload.command)
                    .thenAccept(response -> {
                        // Replace all color codes with & to properly display in Discord
                        JsonObject jsonResponse = new JsonObject();
                        jsonResponse.addProperty("message", MinecraftChatColor.replaceColorKey(response, '&'));
                        future.complete(new DiscordEventResponse(jsonResponse));
                    });
        }, 0);

        return future;
    }
}
