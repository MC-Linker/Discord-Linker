package me.lianecx.discordlinker.common.network.protocol.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.lianecx.discordlinker.common.ConnJson;
import me.lianecx.discordlinker.common.network.protocol.payloads.ChatChannelPayload;
import me.lianecx.discordlinker.common.network.protocol.payloads.InvalidPayloadException;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventJsonResponse;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventResponse;
import me.lianecx.discordlinker.common.util.JsonUtil;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getConnJson;
import static me.lianecx.discordlinker.common.DiscordLinkerCommon.writeConn;

public class RemoveChatChannelDiscordEvent implements LinkerSyncDiscordEvent<ChatChannelPayload> {

    @Override
    public ChatChannelPayload decode(Object[] objects) throws InvalidPayloadException {
        return ChatChannelPayload.decode(objects);
    }

    @Override
    public DiscordEventResponse handle(ChatChannelPayload payload) {
        if(getConnJson() == null) return DiscordEventJsonResponse.CONN_JSON_MISSING;
        getConnJson().getChatChannels().removeIf(channel -> channel.getId().equals(payload.channel.getId()));
        writeConn();

        return DiscordEventJsonResponse.toJson(getConnJson().getChatChannels());
    }
}
