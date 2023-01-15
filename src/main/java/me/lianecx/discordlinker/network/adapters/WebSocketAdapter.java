package me.lianecx.discordlinker.network.adapters;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import express.utils.Status;
import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import me.lianecx.discordlinker.DiscordLinker;
import me.lianecx.discordlinker.network.HttpConnection;
import me.lianecx.discordlinker.network.Route;
import me.lianecx.discordlinker.network.Router;
import org.bukkit.ChatColor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class WebSocketAdapter {

    private final Socket socket;
    private static final Logger LOGGER = DiscordLinker.getPlugin().getLogger();


    public WebSocketAdapter(Map<String, String> auth) {
        Set<Map.Entry<String, JsonElement>> queries = Router.CONNECT_RESPONSE.entrySet();
        String queryString = queries.stream()
                .map(e -> e.getKey() + "=" + e.getValue().getAsString())
                .collect(Collectors.joining("&"));

        IO.Options ioOptions = IO.Options.builder()
                .setAuth(auth)
                .setQuery(queryString)
                .setReconnectionDelayMax(10000)
                .build();

        Socket socket = IO.socket(HttpConnection.BOT_URL, ioOptions);

        socket.on(Socket.EVENT_CONNECT_ERROR, args -> LOGGER.info(ChatColor.RED + "Could not reach the Discord Bot! Reconnecting..."));
        socket.on(Socket.EVENT_CONNECT, args -> LOGGER.info(ChatColor.GREEN + "Connected to the Discord Bot!"));
        socket.on(Socket.EVENT_DISCONNECT, args -> {
            if(args[0].equals("io server disconnect")) {
                LOGGER.info(ChatColor.RED + "Disconnected from the Discord Bot!");
                DiscordLinker.getPlugin().disconnect();
            }
            else LOGGER.info(ChatColor.RED + "Disconnected from the Discord Bot! Reconnecting...");
        });

        socket.onAnyIncoming(args -> {
            System.out.println(args[1]);
            String eventName = (String) args[0];
            JsonObject data = Router.GSON.fromJson(args[1].toString(), JsonObject.class);
            Ack ack = (Ack) args[args.length - 1];

            Route route = Route.getRouteByEventName(eventName);
            if(route == null) {
                ack.call(jsonFromStatus(Status._404));
                return;
            }

            if(route == Route.PUT_FILE) {
                //Special case: File upload (pass body as input stream to function)
                Router.putFile(data, (InputStream) args[2], routerResponse -> this.respond(routerResponse, ack));
            }
            else {
                route.execute(data, routerResponse -> this.respond(routerResponse, ack));
            }
        });

        this.socket = socket;
    }

    public void connect() {
        socket.connect();
    }

    public void disconnect() {
        socket.disconnect();
    }

    private JsonObject jsonFromStatus(Status status) {
        JsonObject json = new JsonObject();
        json.addProperty("status", status.getCode());
        return json;
    }

    private void respond(Router.RouterResponse response, Ack ack) {
        if(response.isAttachment()) {
            //Read files from response and send them
            String path = response.getMessage();
            try {
                byte[] file = Files.readAllBytes(Paths.get(path));
                ack.call(file);
            }
            catch(IOException e) {
                ack.call(jsonFromStatus(Status._500));
            }

            return;
        }

        JsonObject json = jsonFromStatus(response.getStatus());
        if(response.getMessage() != null) {
            JsonObject data = Router.GSON.fromJson(response.getMessage(), JsonObject.class);
            json.add("data", data);
        }
        ack.call(json);
    }
}
