package me.lianecx.discordlinker.common.network.protocol.events;

import me.lianecx.discordlinker.common.network.protocol.payloads.DiscordEventPayload;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventResponse;

import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * A synchronous Discord event contract that executes {@link #handle(DiscordEventPayload)}
 * directly on the current calling thread.
 * <p>
 * This is intended for handlers that do not require explicit main-thread scheduling.
 * The default {@link #handleAsync(DiscordEventPayload)} implementation wraps the direct
 * result in a completed future.
 *
 * @param <T> the decoded payload type for this event
 */
public interface LinkerDirectSyncDiscordEvent<T extends DiscordEventPayload> extends LinkerDiscordEvent<T> {

    /**
     * Handle the event with the given payload.
     *
     * @param payload decoded event payload
     * @return response that will be sent back to the Discord side
     */
    DiscordEventResponse handle(T payload);

    /**
     * Handle the event asynchronously with the given payload.
     * This default implementation invokes {@link #handle(DiscordEventPayload)} immediately
     * on the current thread.
     *
     * @param payload decoded event payload
     * @return completed future containing the handler response, or {@link DiscordEventResponse#UNKNOWN}
     * if handling throws
     */
    default CompletableFuture<DiscordEventResponse> handleAsync(T payload) {
        try {
            return completedFuture(handle(payload));
        } catch (Exception e) {
            return completedFuture(DiscordEventResponse.UNKNOWN);
        }
    }
}
