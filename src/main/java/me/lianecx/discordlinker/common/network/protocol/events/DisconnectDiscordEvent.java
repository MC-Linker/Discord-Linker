package me.lianecx.discordlinker.common.network.protocol.events;

import me.lianecx.discordlinker.common.network.protocol.payloads.EmptyPayload;
import me.lianecx.discordlinker.common.network.protocol.payloads.InvalidPayloadException;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventResponse;
import me.lianecx.discordlinker.common.util.MinecraftChatColor;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.*;

public class DisconnectDiscordEvent implements LinkerDirectSyncDiscordEvent<EmptyPayload> {

    @Override
    public EmptyPayload decode(Object[] objects) throws InvalidPayloadException {
        return new EmptyPayload();
    }

    @Override
    public DiscordEventResponse handle(EmptyPayload payload) {
        if(getConnJson() == null) return DiscordEventResponse.CONN_JSON_MISSING;
        boolean deleted = getConnJson().delete();

        if(deleted) {
            getLogger().info(MinecraftChatColor.YELLOW + "Disconnected from discord...");
            return DiscordEventResponse.SUCCESS;
        }
        else return DiscordEventResponse.IO_ERROR;
    }
}
