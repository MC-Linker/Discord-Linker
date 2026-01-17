package me.lianecx.discordlinker.common.network.protocol.events;

import me.lianecx.discordlinker.common.network.protocol.payloads.DiscordEventPayload;
import me.lianecx.discordlinker.common.network.protocol.payloads.InvalidPayloadException;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventResponse;

public interface DiscordEvent<T extends DiscordEventPayload> {

    /**
    * Decode the payload from an array of objects.
    */
    T decode(Object[] objects) throws InvalidPayloadException;

    /**
     * Handle the event with the given payload.
     */
    DiscordEventResponse handle(T payload);
}
