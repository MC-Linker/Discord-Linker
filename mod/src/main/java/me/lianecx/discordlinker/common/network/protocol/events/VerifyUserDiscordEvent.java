package me.lianecx.discordlinker.common.network.protocol.events;

import me.lianecx.discordlinker.common.network.protocol.payloads.InvalidPayloadException;
import me.lianecx.discordlinker.common.network.protocol.payloads.VerifyUserPayload;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventJsonResponse;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventResponse;

import java.util.UUID;

public class VerifyUserDiscordEvent implements LinkerSyncDiscordEvent<VerifyUserPayload> {

    @Override
    public VerifyUserPayload decode(Object[] objects) {
        if (objects.length != 2) throw new InvalidPayloadException(objects);

        String uuid = (String) objects[0];
        String cpde = (String) objects[1];
        return new VerifyUserPayload(uuid, cpde);
    }

    @Override
    public DiscordEventResponse handle(VerifyUserPayload payload) {
        VerifyCommand.addPlayerToVerificationQueue(payload.uuid, payload.code);
        return DiscordEventJsonResponse.SUCCESS;
    }
}
