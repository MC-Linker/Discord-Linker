package me.lianecx.discordlinker.common.network.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import io.socket.client.Ack;
import io.socket.client.AckWithTimeout;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventFileResponse;
import me.lianecx.discordlinker.common.util.JsonUtil;
import me.lianecx.discordlinker.common.util.MinecraftChatColor;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventJsonResponse;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventResponse;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.*;
import static me.lianecx.discordlinker.common.network.client.ClientManager.BOT_URI;

public final class WebSocketDiscordClient implements DiscordClient {

    public static final int DEFAULT_RECONNECTION_ATTEMPTS = 0; // Default to unlimited reconnection attempts
    private final Dispatcher dispatcher = new Dispatcher();
    private final ExecutorService pool = dispatcher.executorService();
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
        ioOptions.reconnectionDelayMax = 30000;
        ioOptions.reconnectionAttempts = reconnectionAttempts;

        Socket socket = IO.socket(BOT_URI, ioOptions);

        socket.on(Socket.EVENT_CONNECT_ERROR, args -> getLogger().info(MinecraftChatColor.RED + "Could not reach the Discord Bot! Reconnecting..."));
        socket.on(Socket.EVENT_CONNECT, args -> getLogger().info(MinecraftChatColor.GREEN + "Connected to the Discord Bot!"));

        socket.on(Socket.EVENT_DISCONNECT, args -> {
            if(args[0].equals("io server disconnect")) { // Server initiated disconnect, do not reconnect
                getLogger().info(MinecraftChatColor.RED + "Disconnected from the Discord Bot!");
                if(getConnJson() == null) {
                    getLogger().info(MinecraftChatColor.RED + "No connection data found to clean up.");
                    return;
                }
                getConnJson().delete();
            }
            else getLogger().info(MinecraftChatColor.RED + "Disconnected from the Discord Bot! Reconnecting...");
        });

        this.socket = socket;
    }

    public void setReconnectionAttempts(int reconnectionAttempts) {
        socket.io().reconnectionAttempts(reconnectionAttempts);
    }

    @Override
    public void connect(Consumer<Boolean> callback) {
        //Add listeners and remove them after the first event
        AtomicReference<Emitter.Listener> connectListener = new AtomicReference<>();
        AtomicReference<Emitter.Listener> errorListener = new AtomicReference<>();

        connectListener.set(new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                callback.accept(true);
                socket.off(Socket.EVENT_CONNECT, this);
                socket.off(Socket.EVENT_DISCONNECT, errorListener.get());
            }
        });

        errorListener.set(new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                callback.accept(false);
                socket.off(Socket.EVENT_CONNECT, connectListener.get());
                socket.off(Socket.EVENT_DISCONNECT, this);
            }
        });

        socket.on(Socket.EVENT_CONNECT, connectListener.get());
        socket.on(Socket.EVENT_DISCONNECT, errorListener.get());

        socket.connect();
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

                CompletableFuture<DiscordEventResponse> respones = handler.apply(event, data);
                respondToAckFuture(ack, respones);
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
            if (err != null) respondToAck(ack, new DiscordEventJsonResponse(DiscordEventJsonResponse.JsonStatus.ERROR, err.getMessage()));
            else respondToAck(ack, response);
        });
    }

    private void respondToAck(Ack ack, DiscordEventResponse response) {
        if(response instanceof DiscordEventJsonResponse)
            ack.call(((DiscordEventJsonResponse) response).getData());
        else if(response instanceof DiscordEventFileResponse) {
            try {
                byte[] file = Files.readAllBytes(Paths.get(((DiscordEventFileResponse) response).getPath()));
                ack.call(file);
            }
            catch(IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


    @Override
    public void send(String event, Object[] payload) {
        socket.emit(event, payload);
    }

    @Override
    public void send(String event, Object[] payload, Consumer<DiscordEventResponse> callback) {
        socket.emit(event, payload, new AckWithTimeout(5000) {
            @Override
            public void onSuccess(Object... args) {
                // Assume the response is JSON
                if(args.length == 0) {
                    callback.accept(null);
                    return;
                }

                // JSON response
                JsonObject json = JsonUtil.getJsonObjectFromObjects(args);
                if(json == null) {
                    callback.accept(null);
                    return;
                }

                DiscordEventJsonResponse response = new DiscordEventJsonResponse(json);
                callback.accept(response);
            }

            @Override
            public void onTimeout() {
                getLogger().error(MinecraftChatColor.RED + "Request to Discord Bot timed out.");
                callback.accept(null);
            }
        });
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
