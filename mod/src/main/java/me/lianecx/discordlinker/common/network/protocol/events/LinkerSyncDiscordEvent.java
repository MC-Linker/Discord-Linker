package me.lianecx.discordlinker.common.network.protocol.events;

import me.lianecx.discordlinker.common.network.protocol.payloads.DiscordEventPayload;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventJsonResponse;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventResponse;

import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;

public interface LinkerSyncDiscordEvent<T extends DiscordEventPayload> extends LinkerDiscordEvent<T> {

    /**
     * Handle the event with the given payload.
     */
    DiscordEventResponse handle(T payload);

    /**
     * Handle the event asynchronously with the given payload.
     */
    default CompletableFuture<DiscordEventResponse> handleAsync(T payload) {
        try {
            return completedFuture(handle(payload));
        } catch (Exception e) {
            return completedFuture(new DiscordEventJsonResponse(DiscordEventJsonResponse.JsonStatus.ERROR, "Internal error: " + e.getMessage()));
        }
    }
}
