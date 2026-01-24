package me.lianecx.discordlinker.common.network.protocol.events;

import me.lianecx.discordlinker.common.network.protocol.payloads.DiscordEventPayload;
import me.lianecx.discordlinker.common.network.protocol.payloads.InvalidPayloadException;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventResponse;

import java.util.concurrent.CompletableFuture;

public interface LinkerDiscordEvent<T extends DiscordEventPayload> {

    /**
    * Decode the payload from an array of objects.
    */
    T decode(Object[] objects) throws InvalidPayloadException;

    /**
     * Handle the event asynchronously with the given payload.
     */
    CompletableFuture<DiscordEventResponse> handleAsync(T payload);
}
