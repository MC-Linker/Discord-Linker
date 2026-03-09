package me.lianecx.discordlinker.common.network.protocol.events;

import me.lianecx.discordlinker.common.network.protocol.payloads.ChatChannelPayload;
import me.lianecx.discordlinker.common.network.protocol.payloads.InvalidPayloadException;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventResponse;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getConnJson;
import static me.lianecx.discordlinker.common.DiscordLinkerCommon.syncChatConsoleForwarding;

public class AddChatChannelDiscordEvent implements LinkerSyncDiscordEvent<ChatChannelPayload> {

    @Override
    public ChatChannelPayload decode(Object[] objects) throws InvalidPayloadException {
        return ChatChannelPayload.decode(objects);
    }

    @Override
    public DiscordEventResponse handle(ChatChannelPayload payload) {
        if(getConnJson() == null) return DiscordEventResponse.CONN_JSON_MISSING;

        // Remove existing channel with the same ID if it exists
        getConnJson().getChatChannels().removeIf(existingChannel -> existingChannel.getId().equals(payload.channel.getId()));
        // Add the new channel
        getConnJson().getChatChannels().add(payload.channel);
        getConnJson().write();
        syncChatConsoleForwarding();

        return DiscordEventResponse.toJson(getConnJson().getChatChannels());
    }
}
