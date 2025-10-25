package me.lianecx.discordlinker.spigot.network.adapters;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import express.utils.Status;
import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import me.lianecx.discordlinker.spigot.DiscordLinker;
import me.lianecx.discordlinker.spigot.network.Route;
import me.lianecx.discordlinker.spigot.network.Router;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class WebSocketAdapter implements NetworkAdapter {

    public static final int DEFAULT_RECONNECTION_ATTEMPTS = 0; // Default to unlimited reconnection attempts

    private static final DiscordLinker PLUGIN = DiscordLinker.getPlugin();

    private final Socket socket;
    private final Dispatcher dispatcher = new Dispatcher();
    private final ExecutorService pool = dispatcher.executorService();

    public WebSocketAdapter(Map<String, String> auth) {
        this(auth, DEFAULT_RECONNECTION_ATTEMPTS);
    }

    public WebSocketAdapter(Map<String, String> auth, int reconnectionAttempts) {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .dispatcher(dispatcher)
                .readTimeout(1, TimeUnit.MINUTES) // important for HTTP long-polling
                .build();

        Set<Map.Entry<String, JsonElement>> queries = Router.getConnectResponse().entrySet();
        String queryString = queries.stream()
                .filter(e -> !e.getValue().isJsonNull())
                .map(e -> e.getKey() + "=" + e.getValue().getAsString())
                .collect(Collectors.joining("&"));

        IO.Options ioOptions = new IO.Options();
        ioOptions.callFactory = okHttpClient;
        ioOptions.webSocketFactory = okHttpClient;
        ioOptions.auth = auth;
        ioOptions.query = queryString;
        ioOptions.reconnectionDelayMax = 30000;
        ioOptions.reconnectionAttempts = reconnectionAttempts;

        Socket socket = IO.socket(AdapterManager.getBotURI(), ioOptions);

        socket.on(Socket.EVENT_CONNECT_ERROR, args -> PLUGIN.getLogger().info(ChatColor.RED + "Could not reach the Discord Bot! Reconnecting..."));
        socket.on(Socket.EVENT_CONNECT, args -> {
            PLUGIN.getLogger().info(ChatColor.GREEN + "Connected to the Discord Bot!");
            DiscordLinker.getAdapterManager().stopHttp();
        });

        socket.on(Socket.EVENT_DISCONNECT, args -> {
            if(args[0].equals("io server disconnect")) {
                PLUGIN.getLogger().info(ChatColor.RED + "Disconnected from the Discord Bot!");
                PLUGIN.deleteConn();
                DiscordLinker.getAdapterManager().startHttp();
            }
            else PLUGIN.getLogger().info(ChatColor.RED + "Disconnected from the Discord Bot! Reconnecting...");
        });

        socket.onAnyIncoming(args -> {
            String eventName = (String) args[0];
            JsonObject data = new JsonParser().parse(args[1].toString()).getAsJsonObject();

            Bukkit.getLogger().fine("[Socket.io] Event: " + eventName + ", Data: " + data.toString());

            AtomicReference<Ack> ack = new AtomicReference<>(null);
            if(args[args.length - 1] instanceof Ack) ack.set((Ack) args[args.length - 1]); //Optional ack

            Route route = Route.getRouteByEventName(eventName);
            if(route == null) {
                if(ack.get() != null) ack.get().call(jsonFromStatus(Status._404));
                return;
            }

            if(route == Route.PUT_FILE) {
                //Special case: File upload (pass body as input stream to function)
                Router.putFile(data, (InputStream) args[2], routerResponse -> {
                    Bukkit.getLogger().fine("[Socket.io] Event: " + eventName + ", Response: [" + routerResponse.getStatus() + "] " + routerResponse.getMessage());
                    this.respond(routerResponse, ack.get());
                });
            }
            else {
                route.execute(data, routerResponse -> {
                    Bukkit.getLogger().fine("[Socket.io] Event: " + eventName + ", Response: [" + routerResponse.getStatus() + "] " + routerResponse.getMessage());
                    this.respond(routerResponse, ack.get());
                });
            }
        });

        this.socket = socket;
    }

    public void setReconnectionAttempts(int reconnectionAttempts) {
        socket.io().reconnectionAttempts(reconnectionAttempts);
    }

    public void connect(int httpPort, Consumer<Boolean> callback) {
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

    public void disconnect() {
        if(!socket.connected()) return;

        socket.disconnect();

        // https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ExecutorService.html#:~:text=Further%20customization%20is%20also%20possible.%20For%20example%2C%20the%20following%20method%20shuts%20down%20an%20ExecutorService%20in%20two%20phases%2C%20first%20by%20calling%20shutdown%20to%20reject%20incoming%20tasks%2C%20and%20then%20calling%20shutdownNow%2C%20if%20necessary%2C%20to%20cancel%20any%20lingering%20tasks%3A
        pool.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if(!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                pool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if(!pool.awaitTermination(60, TimeUnit.SECONDS))
                    Bukkit.getLogger().severe("Pool did not terminate");
            }
        }
        catch(InterruptedException ex) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    public Socket getSocket() {
        return socket;
    }

    public void send(String event, JsonElement data) {
        socket.emit(event, data);
    }

    public void send(String event, JsonElement data, Ack ack) {
        socket.emit(event, data, ack);
    }

    private JsonObject jsonFromStatus(Status status) {
        JsonObject json = new JsonObject();
        json.addProperty("status", status.getCode());
        return json;
    }

    private void respond(Router.RouterResponse response, Ack ack) {
        if(ack == null) return;

        if(response.isAttachment()) {
            //Read files from response and send them
            String path = response.getMessage();
            try {
                byte[] file = Files.readAllBytes(Paths.get(path));
                ack.call(file);
            }
            catch(IOException err) {
                JsonObject error = jsonFromStatus(Status._500);
                error.addProperty("message", err.toString());
                ack.call(error);
            }

            return;
        }

        JsonObject json = jsonFromStatus(response.getStatus());
        if(response.getMessage() != null) {
            JsonElement data = new JsonParser().parse(response.getMessage());
            json.add("data", data);
        }
        ack.call(json);
    }
}
