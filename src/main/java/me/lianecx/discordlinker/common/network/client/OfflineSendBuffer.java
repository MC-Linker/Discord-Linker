package me.lianecx.discordlinker.common.network.client;

import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventResponse;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.ToLongFunction;

/**
 * Holds selected events while the Discord Bot is offline and delivers them once it is back.
 * <ul>
 *   <li>Only {@link #BUFFERABLE_EVENTS} are accepted, at most {@link #MAX_BUFFERED_EVENTS} of them (when full, the
 *       oldest is dropped).</li>
 *   <li>Each entry gets a fixed deadline when it is offered: {@code callbackTtlMs} if it has a callback (someone is
 *       waiting for it), otherwise {@link #BUFFERED_EVENT_TTL_MS}. The deadline is never reset; when it passes, the
 *       entry is removed.</li>
 *   <li>{@link #flush()} delivers oldest first, one entry at a time, and stops if the connection drops again.</li>
 *   <li>An entry without a callback whose event is still rate-limited waits for the rate limit to end. An entry with a
 *       callback gets {@link DiscordEventResponse#RATE_LIMITED} instead.</li>
 *   <li>Dropped, evicted and expired entries complete their callback with {@code null}.</li>
 * </ul>
 * Delivery and expiry run on one internal thread.
 */
final class OfflineSendBuffer {

    /** Fire-and-forget events worth delivering late after a short disconnect. Everything else fails fast while offline. */
    private static final Set<String> BUFFERABLE_EVENTS = new HashSet<>(Arrays.asList("chat", "remove-synced-role"));
    private static final int MAX_BUFFERED_EVENTS = 100;
    private static final long BUFFERED_EVENT_TTL_MS = 2 * 60_000;

    /** Emits an event on the socket without any buffering or rate-limit checks. */
    interface Sink {
        /** @param callback nullable */
        void emit(String event, Object[] payload, Consumer<DiscordEventResponse> callback);
    }

    private static final class PendingSend {
        final String event;
        final Object[] payload;
        final Consumer<DiscordEventResponse> callback; // nullable
        ScheduledFuture<?> expiry; // guarded by pendingSends

        PendingSend(String event, Object[] payload, Consumer<DiscordEventResponse> callback) {
            this.event = event;
            this.payload = payload;
            this.callback = callback;
        }

        void complete(DiscordEventResponse response) {
            if(callback != null) callback.accept(response);
        }
    }

    private final long callbackTtlMs;
    private final BooleanSupplier connected;
    private final ToLongFunction<String> rateLimitRemainingMs;
    private final Sink sink;

    private final ScheduledThreadPoolExecutor scheduler = createScheduler();

    // All guarded by pendingSends.
    private final Deque<PendingSend> pendingSends = new ArrayDeque<>();
    private boolean shutdown;
    private ScheduledFuture<?> rateLimitRetry;

    /** @param callbackTtlMs deadline for entries with a callback; should match the ack timeout their callers expect */
    OfflineSendBuffer(long callbackTtlMs, BooleanSupplier connected, ToLongFunction<String> rateLimitRemainingMs, Sink sink) {
        this.callbackTtlMs = callbackTtlMs;
        this.connected = connected;
        this.rateLimitRemainingMs = rateLimitRemainingMs;
        this.sink = sink;
    }

    private static ScheduledThreadPoolExecutor createScheduler() {
        ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1, runnable -> {
            Thread thread = new Thread(runnable, "DiscordLinker-SendBuffer");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.setRemoveOnCancelPolicy(true);
        return scheduler;
    }

    /**
     * Queues an event. Call while the bot is offline.
     * @return false if the event is not buffered (or the buffer is shut down); the caller should drop it.
     */
    boolean offer(String event, Object[] payload, Consumer<DiscordEventResponse> callback) {
        if(!BUFFERABLE_EVENTS.contains(event)) return false;

        PendingSend pending = new PendingSend(event, payload, callback);
        PendingSend evicted = null;
        synchronized(pendingSends) {
            if(shutdown) return false;

            if(pendingSends.size() >= MAX_BUFFERED_EVENTS) {
                evicted = pendingSends.pollFirst();
                evicted.expiry.cancel(false);
            }
            pendingSends.addLast(pending);
            long deadlineMs = callback != null ? callbackTtlMs : BUFFERED_EVENT_TTL_MS;
            pending.expiry = scheduler.schedule(() -> expire(pending), deadlineMs, TimeUnit.MILLISECONDS);

            // The connection may have come back (and flushed) after the caller's offline check.
            if(connected.getAsBoolean()) scheduler.execute(this::deliver);
        }

        if(evicted != null) evicted.complete(null);
        return true;
    }

    /** Delivers everything that is queued. Call once the connection is back. */
    void flush() {
        synchronized(pendingSends) {
            if(!shutdown) scheduler.execute(this::deliver);
        }
    }

    /** Drops everything that is queued; callbacks get {@code null}. */
    void clear() {
        for(PendingSend p : drain()) p.complete(null);
    }

    /** Drops everything that is queued (callbacks get {@code null}), rejects further offers and stops the thread. */
    void shutdown() {
        List<PendingSend> dropped;
        synchronized(pendingSends) {
            shutdown = true;
            dropped = drain();
        }
        scheduler.shutdownNow();
        for(PendingSend p : dropped) p.complete(null);
    }

    /** Runs on the scheduler thread, so deliveries never overlap. */
    private void deliver() {
        PendingSend next;
        while((next = takeNext()) != null) {
            if(next.callback != null && rateLimitRemainingMs.applyAsLong(next.event) > 0)
                next.complete(DiscordEventResponse.RATE_LIMITED);
            else
                sink.emit(next.event, next.payload, next.callback);
        }
    }

    /**
     * Removes and returns the oldest deliverable entry, or {@code null} if the bot is offline or nothing can be sent.
     * An entry only leaves the queue here, right before it is emitted, so the rest keep their deadlines if the
     * connection drops mid-flush. Rate-limited entries without a callback are skipped and retried when the earliest
     * of those rate limits ends.
     */
    private PendingSend takeNext() {
        synchronized(pendingSends) {
            if(shutdown || !connected.getAsBoolean()) return null;

            long retryInMs = Long.MAX_VALUE;
            for(Iterator<PendingSend> it = pendingSends.iterator(); it.hasNext(); ) {
                PendingSend p = it.next();
                long wait = p.callback == null ? rateLimitRemainingMs.applyAsLong(p.event) : 0;
                if(wait > 0) {
                    retryInMs = Math.min(retryInMs, wait);
                    continue;
                }
                it.remove();
                p.expiry.cancel(false);
                return p;
            }

            if(rateLimitRetry != null) rateLimitRetry.cancel(false);
            rateLimitRetry = retryInMs == Long.MAX_VALUE ? null : scheduler.schedule(this::deliver, retryInMs, TimeUnit.MILLISECONDS);
            return null;
        }
    }

    /** Deadline reached: if the entry is still queued, remove it so it can never be sent, and fail its callback. */
    private void expire(PendingSend pending) {
        boolean removed;
        synchronized(pendingSends) {
            removed = pendingSends.remove(pending);
        }
        if(removed) pending.complete(null);
    }

    /** Empties the queue and cancels the expiry timers of the removed entries. */
    private List<PendingSend> drain() {
        synchronized(pendingSends) {
            List<PendingSend> drained = new ArrayList<>(pendingSends);
            pendingSends.clear();
            for(PendingSend p : drained) p.expiry.cancel(false);
            return drained;
        }
    }
}
