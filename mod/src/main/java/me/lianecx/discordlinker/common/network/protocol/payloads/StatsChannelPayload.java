package me.lianecx.discordlinker.common.network.protocol.payloads;

import com.google.gson.JsonObject;
import me.lianecx.discordlinker.common.ConnJson;
import me.lianecx.discordlinker.common.util.JsonUtil;
import org.jetbrains.annotations.Contract;

public class StatsChannelPayload implements DiscordEventPayload {
    public final ConnJson.StatsChannel channel;

    public StatsChannelPayload(ConnJson.StatsChannel channel) {
        this.channel = channel;
    }

    @Contract("_ -> new")
    public static StatsChannelPayload decode(Object[] objects) throws InvalidPayloadException {
        JsonObject payload = JsonUtil.getJsonObjectFromObjects(objects);
        if(payload == null) throw new InvalidPayloadException(objects);

        ConnJson.StatsChannel channel = JsonUtil.fromJson(payload, ConnJson.StatsChannel.class);
        if(channel == null) throw new InvalidPayloadException(objects);
        return new StatsChannelPayload(channel);
    }
}
