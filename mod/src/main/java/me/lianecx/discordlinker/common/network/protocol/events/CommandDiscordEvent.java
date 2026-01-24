package me.lianecx.discordlinker.common.network.protocol.events;

import com.google.gson.JsonObject;
import me.lianecx.discordlinker.common.network.protocol.payloads.CommandPayload;
import me.lianecx.discordlinker.common.network.protocol.payloads.InvalidPayloadException;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventJsonResponse;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventResponse;
import me.lianecx.discordlinker.common.util.JsonUtil;
import me.lianecx.discordlinker.common.util.MinecraftChatColor;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.concurrent.CompletableFuture;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.*;
import static me.lianecx.discordlinker.common.util.URLEncoderUtil.decodeURL;

public class CommandDiscordEvent implements LinkerDiscordEvent<CommandPayload> {

    @Override
    public CommandPayload decode(Object[] objects) {
        JsonObject payload = JsonUtil.getJsonObjectFromObjects(objects);
        if (payload == null) throw new InvalidPayloadException(objects);

        String command = decodeURL(payload.get("cmd").getAsString());
        return new CommandPayload(command);
    }

    @Override
    public CompletableFuture<DiscordEventResponse> handleAsync(CommandPayload payload) {
        CompletableFuture<DiscordEventResponse> future = new CompletableFuture<>();
        getScheduler().runDelayedSync(() -> {
            try {
                String cmd = URLDecoder.decode(payload.command, "utf-8");
                getLogger().info(MinecraftChatColor.AQUA + "Command from Discord: /" + cmd);
                String response = getServer().runCommand(cmd.trim());

                // Replace all color codes with & to properly display in Discord
                future.complete(new DiscordEventJsonResponse(DiscordEventJsonResponse.JsonStatus.SUCCESS, MinecraftChatColor.replaceColorKey(response, '&')));
            }
            catch(UnsupportedEncodingException err) {
                future.complete(new DiscordEventJsonResponse(DiscordEventJsonResponse.JsonStatus.ERROR, err.toString()));
            }
        }, 0);

        return future;
    }
}
