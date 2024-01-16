package me.lianecx.discordlinker.network.adapters;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import express.utils.Status;
import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import me.lianecx.discordlinker.DiscordLinker;
import me.lianecx.discordlinker.network.Route;
import me.lianecx.discordlinker.network.Router;
import org.bukkit.ChatColor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class WebSocketAdapter implements NetworkAdapter {

    private final Socket socket;
    private static final DiscordLinker PLUGIN = DiscordLinker.getPlugin();

    public WebSocketAdapter(Map<String, String> auth) {
        Set<Map.Entry<String, JsonElement>> queries = Router.getConnectResponse().entrySet();
        String queryString = queries.stream()
                .filter(e -> !e.getValue().isJsonNull())
                .map(e -> e.getKey() + "=" + e.getValue().getAsString())
                .collect(Collectors.joining("&"));

        IO.Options ioOptions = IO.Options.builder()
                .setAuth(auth)
                .setQuery(queryString)
                .setReconnectionDelayMax(10000)
                .build();

        Socket socket = IO.socket(HttpAdapter.BOT_URI, ioOptions);

        socket.on(Socket.EVENT_CONNECT_ERROR, args -> PLUGIN.getLogger().info(ChatColor.RED + "Could not reach the Discord Bot! Reconnecting..."));
        socket.on(Socket.EVENT_CONNECT, args -> {
            PLUGIN.getLogger().info(ChatColor.GREEN + "Connected to the Discord Bot!");
            DiscordLinker.getAdapterManager().stopHttp();
        });

        socket.on(Socket.EVENT_DISCONNECT, args -> {
            if(args[0].equals("io server disconnect") || args[0].equals("io client disconnect")) {
                PLUGIN.getLogger().info(ChatColor.RED + "Disconnected from the Discord Bot!");
                PLUGIN.deleteConn();
                DiscordLinker.getAdapterManager().startHttp();
            }
            else PLUGIN.getLogger().info(ChatColor.RED + "Disconnected from the Discord Bot! Reconnecting...");
        });

        socket.onAnyIncoming(args -> {
            String eventName = (String) args[0];
            JsonObject data = new JsonParser().parse(args[1].toString()).getAsJsonObject();

            AtomicReference<Ack> ack = new AtomicReference<>(null);
            if(args[args.length - 1] instanceof Ack) ack.set((Ack) args[args.length - 1]); //Optional ack

            Route route = Route.getRouteByEventName(eventName);
            if(route == null) {
                if(ack.get() != null) ack.get().call(jsonFromStatus(Status._404));
                return;
            }

            if(route == Route.PUT_FILE) {
                //Special case: File upload (pass body as input stream to function)
                Router.putFile(data, (InputStream) args[2], routerResponse -> this.respond(routerResponse, ack.get()));
            }
            else {
                route.execute(data, routerResponse -> this.respond(routerResponse, ack.get()));
            }
        });

        this.socket = socket;
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
                socket.off(Socket.EVENT_CONNECT_ERROR, errorListener.get());
                socket.off(Socket.EVENT_DISCONNECT, errorListener.get());
            }
        });

        errorListener.set(new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                callback.accept(false);
                socket.off(Socket.EVENT_CONNECT, connectListener.get());
                socket.off(Socket.EVENT_CONNECT_ERROR, this);
                socket.off(Socket.EVENT_DISCONNECT, this);
            }
        });

        socket.on(Socket.EVENT_CONNECT, connectListener.get());
        socket.on(Socket.EVENT_CONNECT_ERROR, errorListener.get());
        socket.on(Socket.EVENT_DISCONNECT, errorListener.get());

        socket.connect();
    }

    public void disconnect() {
        socket.disconnect();
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
