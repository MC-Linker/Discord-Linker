package me.lianecx.discordlinker.common.network.protocol.payloads;

import com.google.gson.JsonObject;
import me.lianecx.discordlinker.common.ConnJson;
import me.lianecx.discordlinker.common.util.JsonUtil;
import org.jetbrains.annotations.Contract;

public class ChatChannelPayload implements DiscordEventPayload {
    public final ConnJson.ChatChannel channel;

    public ChatChannelPayload(ConnJson.ChatChannel channel) {
        this.channel = channel;
    }

    @Contract("_ -> new")
    public static ChatChannelPayload decode(Object[] objects) throws InvalidPayloadException {
        JsonObject payload = JsonUtil.parseJsonObject(objects);
        if(payload == null) throw new InvalidPayloadException(objects);

        ConnJson.ChatChannel channel = JsonUtil.fromJson(payload, ConnJson.ChatChannel.class);
        if(channel == null) throw new InvalidPayloadException(objects);
        return new ChatChannelPayload(channel);
    }
}
