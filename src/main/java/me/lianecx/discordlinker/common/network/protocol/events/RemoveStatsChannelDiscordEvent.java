package me.lianecx.discordlinker.common.network.protocol.events;

import me.lianecx.discordlinker.common.network.protocol.payloads.InvalidPayloadException;
import me.lianecx.discordlinker.common.network.protocol.payloads.StatsChannelPayload;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventResponse;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.*;

public class RemoveStatsChannelDiscordEvent implements LinkerSyncDiscordEvent<StatsChannelPayload> {

    @Override
    public StatsChannelPayload decode(Object[] objects) throws InvalidPayloadException {
        return StatsChannelPayload.decode(objects);
    }

    @Override
    public DiscordEventResponse handle(StatsChannelPayload payload) {
        if(getConnJson() == null) return DiscordEventResponse.CONN_JSON_MISSING;
        getConnJson().getStatsChannels().removeIf(channel -> channel.getId().equals(payload.channel.getId()));
        getConnJson().write();

        return DiscordEventResponse.toJson(getConnJson().getStatsChannels());
    }
}
