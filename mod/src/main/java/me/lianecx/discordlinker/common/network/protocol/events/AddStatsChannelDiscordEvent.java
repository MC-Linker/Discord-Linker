package me.lianecx.discordlinker.common.network.protocol.events;

import com.google.gson.JsonElement;
import me.lianecx.discordlinker.common.network.protocol.payloads.InvalidPayloadException;
import me.lianecx.discordlinker.common.network.protocol.payloads.StatsChannelPayload;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventJsonResponse;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventResponse;
import me.lianecx.discordlinker.common.util.JsonUtil;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.*;

public class AddStatsChannelDiscordEvent implements LinkerSyncDiscordEvent<StatsChannelPayload> {

    @Override
    public StatsChannelPayload decode(Object[] objects) throws InvalidPayloadException {
        return StatsChannelPayload.decode(objects);
    }

    @Override
    public DiscordEventResponse handle(StatsChannelPayload payload) {
        if(getConnJson() == null) return DiscordEventJsonResponse.CONN_JSON_MISSING;

        // Remove existing channel with the same ID if it exists
        getConnJson().getStatsChannels().removeIf(existingChannel -> existingChannel.getId().equals(payload.channel.getId()));
        // Add the new channel
        getConnJson().getStatsChannels().add(payload.channel);
        getConnJson().write();

        return DiscordEventJsonResponse.toJson(getConnJson().getStatsChannels());
    }
}
