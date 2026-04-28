package me.lianecx.discordlinker.common.network.protocol.events;

import me.lianecx.discordlinker.common.network.protocol.payloads.ChatChannelPayload;
import me.lianecx.discordlinker.common.network.protocol.payloads.InvalidPayloadException;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventResponse;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getConnJson;
import static me.lianecx.discordlinker.common.DiscordLinkerCommon.syncChatConsoleForwarding;

public class RemoveChatChannelDiscordEvent implements LinkerDirectSyncDiscordEvent<ChatChannelPayload> {

    @Override
    public ChatChannelPayload decode(Object[] objects) throws InvalidPayloadException {
        return ChatChannelPayload.decode(objects);
    }

    @Override
    public DiscordEventResponse handle(ChatChannelPayload payload) {
        if(getConnJson() == null) return DiscordEventResponse.CONN_JSON_MISSING;
        getConnJson().getChatChannels().removeIf(channel -> channel.getId().equals(payload.channel.getId()));
        getConnJson().write();
        syncChatConsoleForwarding();

        return DiscordEventResponse.toJson(getConnJson().getChatChannels());
    }
}
