package me.lianecx.discordlinker.common.network.protocol.events;

import me.lianecx.discordlinker.common.network.protocol.payloads.EmptyPayload;
import me.lianecx.discordlinker.common.network.protocol.payloads.InvalidPayloadException;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventJsonResponse;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventResponse;
import me.lianecx.discordlinker.common.util.MinecraftChatColor;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.deleteConn;
import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getLogger;

public class DisconnectDiscordEvent implements LinkerSyncDiscordEvent<EmptyPayload> {

    @Override
    public EmptyPayload decode(Object[] objects) throws InvalidPayloadException {
        return new EmptyPayload();
    }

    @Override
    public DiscordEventResponse handle(EmptyPayload payload) {
        boolean deleted = deleteConn();

        if(deleted) {
            getLogger().info(MinecraftChatColor.YELLOW + "Disconnected from discord...");
            return DiscordEventJsonResponse.SUCCESS;
        }
        else return new DiscordEventJsonResponse(DiscordEventJsonResponse.JsonStatus.ERROR, "Could not delete connection file");
    }
}
