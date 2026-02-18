package me.lianecx.discordlinker.common.network.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import io.socket.client.Ack;
import io.socket.client.AckWithTimeout;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import me.lianecx.discordlinker.common.util.JsonUtil;
import me.lianecx.discordlinker.common.util.MinecraftChatColor;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventResponse;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.*;
import static me.lianecx.discordlinker.common.network.client.ClientManager.BOT_URI;

public final class WebSocketDiscordClient implements DiscordClient {

    public static final int DEFAULT_RECONNECTION_ATTEMPTS = Integer.MAX_VALUE; // Default to unlimited reconnection attempts

    private final Dispatcher dispatcher = new Dispatcher();
    private final ExecutorService pool = dispatcher.executorService();
    private final Map<String, Long> rateLimitedUntil = new ConcurrentHashMap<>();
    private Socket socket;

    public WebSocketDiscordClient(Map<String, String> auth, Map<String, String> query) {
        this(auth, query, DEFAULT_RECONNECTION_ATTEMPTS);
    }

    public WebSocketDiscordClient(Map<String, String> auth, Map<String, String> query, int reconnectionAttempts) {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .dispatcher(dispatcher)
                .readTimeout(1, TimeUnit.MINUTES) // important for HTTP long-polling
                .build();

        String queryString = query.entrySet().stream()
                .filter(e -> e.getValue() != null)
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));

        IO.Options ioOptions = new IO.Options();
        ioOptions.transports = new String[] { "websocket" }; // Force WebSocket transport (avoids issues)
        ioOptions.callFactory = okHttpClient;
        ioOptions.webSocketFactory = okHttpClient;
        ioOptions.auth = auth;
        ioOptions.query = queryString;
        ioOptions.reconnectionDelayMax = 32000;
        ioOptions.reconnectionAttempts = reconnectionAttempts;

        Socket socket = IO.socket(BOT_URI, ioOptions);

        socket.on(Socket.EVENT_CONNECT_ERROR, args -> {
            getLogger().debug("[Socket.io] Connect error: " + Arrays.toString(args));

            try {
                String message = JsonUtil.parseJsonObject(args).get("message").getAsString();
                // Handled elsewhere, no reconnect
                if(message.equals("Unauthorized") || message.equals("Server Error")) return;
            }
            catch(Exception ignored) {}

            getLogger().info(MinecraftChatColor.RED + "Could not reach the Discord Bot! Reconnecting...");
            socket.connect(); // Need to manually reconnect
        });
        socket.on(Socket.EVENT_CONNECT, args -> {
            getLogger().debug("[Socket.io] Connected with args: " + Arrays.toString(args));
            getLogger().info(MinecraftChatColor.GREEN + "Connected to the Discord Bot!");
        });

        socket.on(Socket.EVENT_DISCONNECT, args -> {
            getLogger().debug("[Socket.io] Disconnected with args: " + Arrays.toString(args));

            // Server or client initiated disconnect, no reconnect
            if(args[0].equals("io server disconnect") || args[0].equals("io client disconnect"))
                getLogger().info(MinecraftChatColor.RED + "Disconnected from the Discord Bot!");
            else getLogger().info(MinecraftChatColor.RED + "Disconnected from the Discord Bot! Reconnecting...");

            // Server disconnected, meaning someone ran /disconnect
            if(args[0].equals("io server disconnect")) {
                if(getConnJson() == null) {
                    getLogger().info(MinecraftChatColor.YELLOW + "No connection data found to clean up.");
                    return;
                }
                getConnJson().delete();
            }
        });

        this.socket = socket;
    }

    public void setReconnectionAttempts(int reconnectionAttempts) {
        socket.io().reconnectionAttempts(reconnectionAttempts);
    }

    @Override
    public CompletableFuture<Boolean> connect() {
        if(isConnected()) socket.disconnect(); // Reset connection if already connected

        CompletableFuture<Boolean> future = new CompletableFuture<>();
        AtomicBoolean done = new AtomicBoolean(false);

        final Emitter.Listener[] connectListener = new Emitter.Listener[1];
        final Emitter.Listener[] errorListener = new Emitter.Listener[1];

        Runnable listenerCleanup = () -> {
            socket.off(Socket.EVENT_CONNECT, connectListener[0]);
            socket.off(Socket.EVENT_CONNECT_ERROR, errorListener[0]);
        };

        connectListener[0] = args -> {
            if(done.compareAndSet(false, true)) {
                listenerCleanup.run();
                future.complete(true);
            }
        };

        errorListener[0] = args -> {
            if(done.compareAndSet(false, true)) {
                listenerCleanup.run();
                future.complete(false);
            }
        };

        socket.on(Socket.EVENT_CONNECT, connectListener[0]);
        socket.on(Socket.EVENT_CONNECT_ERROR, errorListener[0]);

        socket.connect();
        return future;
    }


    @Override
    public void on(String event, Function<Object[], CompletableFuture<DiscordEventResponse>> handler) {
        socket.on(event, args -> {
            try {
                Ack ack = getAckFromArgs(args);

                // Remove the last argument if it's an Ack
                Object[] data = args;
                if(ack != null) {
                    data = new Object[args.length - 1];
                    System.arraycopy(args, 0, data, 0, args.length - 1);
                }

                CompletableFuture<DiscordEventResponse> response = handler.apply(data);
                respondToAckFuture(ack, response);
            }
            catch(JsonSyntaxException ignored) {}
        });
    }

    @Override
    public void once(String event, Function<Object[], CompletableFuture<DiscordEventResponse>> handler) {
        socket.once(event, args -> {
            try {
                Ack ack = getAckFromArgs(args);

                // Remove the last argument if it's an Ack
                Object[] data = args;
                if(ack != null) {
                    data = new Object[args.length - 1];
                    System.arraycopy(args, 0, data, 0, args.length - 1);
                }

                CompletableFuture<DiscordEventResponse> response = handler.apply(data);
                respondToAckFuture(ack, response);
            }
            catch(JsonSyntaxException ignored) {}
        });
    }

    @Override
    public void onAny(BiFunction<String, Object[], CompletableFuture<DiscordEventResponse>> handler) {
        socket.onAnyIncoming(args -> {
            try {
                getLogger().debug("[Socket.io] Received raw event: " + Arrays.toString(args));
                if (args == null || args.length < 1) return;
                if (!(args[0] instanceof String)) return;

                String event = (String) args[0];
                Ack ack = getAckFromArgs(args);

                // Remove the first argument (event name) and the last argument if it's an Ack
                Object[] data;
                int dataLength = args.length - 1; // Exclude event name
                if(ack != null) dataLength--; // Exclude Ack if present
                data = new Object[dataLength];
                System.arraycopy(args, 1, data, 0, dataLength);

                getLogger().debug("[Socket.io] Parsed event: " + event + ", data: " + Arrays.toString(data) + ", ack: " + (ack != null));

                CompletableFuture<DiscordEventResponse> response = handler.apply(event, data);
                respondToAckFuture(ack, response);
            }
            catch(JsonSyntaxException ignored) {} // Ignore invalid messages
        });
    }

    private Ack getAckFromArgs(Object[] args) {
        if(args.length == 0) return null;
        Object lastArg = args[args.length - 1];
        if(lastArg instanceof Ack) return (Ack) lastArg;
        return null;
    }

    private void respondToAckFuture(Ack ack, CompletableFuture<DiscordEventResponse> future) {
        if(ack == null || future == null) return;

        future.whenComplete((response, err) -> {
            if(err != null) respondToAck(ack, DiscordEventResponse.UNKNOWN);
            else respondToAck(ack, response);
        });
    }

    private void respondToAck(Ack ack, DiscordEventResponse response) {
        getLogger().debug("[Socket.io] Responding to Ack with JSON: " + response.getData());
        ack.call(response.getData());
    }


    @Override
    public void send(String event, Object[] payload) {
        if(isRateLimited(event)) {
            getLogger().warn("[Socket.io] Dropping event '" + event + "' due to rate limit.");
//            return; TODO don't drop for now
        }

        getLogger().debug("[Socket.io] Emitting event: " + event + ", payload: " + Arrays.toString(payload));
        socket.emit(event, payload);
    }

    @Override
    public void send(String event, Object[] payload, Consumer<DiscordEventResponse> callback) {
        if(isRateLimited(event)) {
            getLogger().warn("[Socket.io] Dropping event '" + event + "' due to rate limit.");
//            callback.accept(DiscordEventResponse.RATE_LIMITED);
//            return; TODO don't drop for now
        }

        getLogger().debug("[Socket.io] Emitting event with callback: " + event + ", payload: " + Arrays.toString(payload));
        socket.emit(event, payload, new AckWithTimeout(5000) {
            @Override
            public void onSuccess(Object... args) {
                getLogger().debug("[Socket.io] Received raw Ack response: " + Arrays.toString(args));
                // Assume the response is JSON
                if(args.length == 0) {
                    callback.accept(null);
                    return;
                }

                // JSON response
                JsonObject json = JsonUtil.parseJsonObject(args);
                if(json == null) {
                    callback.accept(null);
                    return;
                }

                DiscordEventResponse response = new DiscordEventResponse(json, true);
                getLogger().debug("[Socket.io] Parsed Ack response: " + response.getData());

                // If rate-limited, store the per-event expiry and forward the response
                if(response.isRateLimited()) {
                    long retryMs = response.getRetryMs();
                    if(retryMs > 0) rateLimitedUntil.put(event, System.currentTimeMillis() + retryMs);
                    getLogger().warn("[Socket.io] Rate limited on '" + event + "' for " + retryMs + "ms.");
                }

                callback.accept(response);
            }

            @Override
            public void onTimeout() {
                getLogger().error(MinecraftChatColor.RED + "Request to Discord Bot timed out.");
                callback.accept(null);
            }
        });
    }

    private boolean isRateLimited(String event) {
        Long expiry = rateLimitedUntil.get(event);
        if(expiry == null) return false;
        if(System.currentTimeMillis() >= expiry) {
            rateLimitedUntil.remove(event);
            return false;
        }
        return true;
    }

    @Override
    public void disconnect() {
        if(!isConnected()) return;

        socket.disconnect();

        // https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ExecutorService.html#:~:text=Further%20customization%20is%20also%20possible.%20For%20example%2C%20the%20following%20method%20shuts%20down%20an%20ExecutorService%20in%20two%20phases%2C%20first%20by%20calling%20shutdown%20to%20reject%20incoming%20tasks%2C%20and%20then%20calling%20shutdownNow%2C%20if%20necessary%2C%20to%20cancel%20any%20lingering%20tasks%3A
        pool.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if(!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                pool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if(!pool.awaitTermination(60, TimeUnit.SECONDS))
                    getLogger().error("Pool did not terminate");
            }
        }
        catch(InterruptedException ex) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public boolean isConnected() {
        return socket != null && socket.connected();
    }
}
