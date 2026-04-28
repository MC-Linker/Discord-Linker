package me.lianecx.discordlinker.common.network.protocol.events;

import me.lianecx.discordlinker.common.network.protocol.payloads.DiscordEventPayload;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventResponse;

import java.util.concurrent.CompletableFuture;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getScheduler;

/**
 * A synchronous Discord event contract whose handling is always scheduled onto
 * the server main thread.
 * <p>
 * Use this for handlers that access non-thread-safe Minecraft APIs.
 * The default {@link #handleAsync(DiscordEventPayload)} implementation delegates
 * to {@link #handle(DiscordEventPayload)} through the platform scheduler.
 *
 * @param <T> the decoded payload type for this event
 */
public interface LinkerScheduledSyncDiscordEvent<T extends DiscordEventPayload> extends LinkerDiscordEvent<T> {

    /**
     * Handle the event with the given payload.
     * This method is guaranteed to be called on the main server thread.
     *
     * @param payload decoded event payload
     * @return response that will be sent back to the Discord side
     */
    DiscordEventResponse handle(T payload);

    /**
     * Schedules {@link #handle} on the main server thread and returns a future that completes
     * with the result. This ensures all Minecraft API calls within {@link #handle} are
     * thread-safe.
     *
     * @param payload decoded event payload
     * @return future completed with the handler response, or {@link DiscordEventResponse#UNKNOWN}
     * if handling throws
     */
    default CompletableFuture<DiscordEventResponse> handleAsync(T payload) {
        CompletableFuture<DiscordEventResponse> future = new CompletableFuture<>();
        getScheduler().runSync(() -> {
            try {
                future.complete(handle(payload));
            } catch (Exception e) {
                future.complete(DiscordEventResponse.UNKNOWN);
            }
        });
        return future;
    }
}
