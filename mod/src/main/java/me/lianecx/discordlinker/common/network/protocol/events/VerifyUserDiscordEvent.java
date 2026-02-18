package me.lianecx.discordlinker.common.network.protocol.events;

import com.google.gson.JsonObject;
import me.lianecx.discordlinker.common.commands.VerifyCommand;
import me.lianecx.discordlinker.common.network.protocol.payloads.InvalidPayloadException;
import me.lianecx.discordlinker.common.network.protocol.payloads.VerifyUserPayload;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventResponse;
import me.lianecx.discordlinker.common.util.JsonUtil;

public class VerifyUserDiscordEvent implements LinkerSyncDiscordEvent<VerifyUserPayload> {

    @Override
    public VerifyUserPayload decode(Object[] objects) {
        JsonObject payload = JsonUtil.parseJsonObject(objects);
        if (payload == null) throw new InvalidPayloadException(objects);

        String uuid = payload.get("uuid").getAsString();
        String code = payload.get("code").getAsString();
        return new VerifyUserPayload(uuid, code);
    }

    @Override
    public DiscordEventResponse handle(VerifyUserPayload payload) {
        VerifyCommand.addPlayerToVerificationQueue(payload.uuid, payload.code);
        return DiscordEventResponse.SUCCESS;
    }
}
