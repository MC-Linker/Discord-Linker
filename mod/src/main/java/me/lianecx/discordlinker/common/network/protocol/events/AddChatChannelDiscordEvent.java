package me.lianecx.discordlinker.common.network.protocol.events;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.lianecx.discordlinker.common.network.protocol.payloads.ChatChannelPayload;
import me.lianecx.discordlinker.common.network.protocol.payloads.InvalidPayloadException;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventJsonResponse;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventResponse;
import me.lianecx.discordlinker.common.util.JsonUtil;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getConnJson;
import static me.lianecx.discordlinker.common.DiscordLinkerCommon.writeConn;

public class AddChatChannelDiscordEvent implements LinkerSyncDiscordEvent<ChatChannelPayload> {

    @Override
    public ChatChannelPayload decode(Object[] objects) throws InvalidPayloadException {
        return ChatChannelPayload.decode(objects);
    }

    @Override
    public DiscordEventResponse handle(ChatChannelPayload payload) {
        if(getConnJson() == null) return DiscordEventJsonResponse.CONN_JSON_MISSING;

        // Remove existing channel with the same ID if it exists
        getConnJson().getChatChannels().removeIf(existingChannel -> existingChannel.getId().equals(payload.channel.getId()));
        // Add the new channel
        getConnJson().getChatChannels().add(payload.channel);
        writeConn();

        return DiscordEventJsonResponse.toJson(getConnJson().getChatChannels());
    }
}
