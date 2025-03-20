package me.lianecx.discordlinker.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import express.utils.Status;
import io.socket.client.Ack;
import io.socket.client.AckWithTimeout;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import me.lianecx.discordlinker.DiscordLinker;
import me.lianecx.discordlinker.events.TeamChangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class WebSocketConnection {

    //If the port is not set in the config, default to 80
    public static final int BOT_PORT = DiscordLinker.getPlugin().getConfig().getInt("bot-port", -1) > 0 ? DiscordLinker.getPlugin().getConfig().getInt("bot-port") : 80;
    public static final URI BOT_URI = URI.create("http://api.mclinker.com:" + BOT_PORT);
    private static final String PLUGIN_VERSION = DiscordLinker.getPlugin().getDescription().getVersion();

    private static final DiscordLinker PLUGIN = DiscordLinker.getPlugin();
    private Socket socket;

    public WebSocketConnection(String token) {
        this(Collections.singletonMap("token", token));
    }

    public WebSocketConnection(Map<String, String> auth) {
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

        Socket socket = IO.socket(BOT_URI, ioOptions);

        socket.on(Socket.EVENT_CONNECT_ERROR, args -> PLUGIN.getLogger().info(ChatColor.RED + "Could not reach the Discord Bot! Reconnecting..."));
        socket.on(Socket.EVENT_CONNECT, args -> PLUGIN.getLogger().info(ChatColor.GREEN + "Connected to the Discord Bot!"));

        socket.on(Socket.EVENT_DISCONNECT, args -> {
            if(args[0].equals("io server disconnect")) {
                PLUGIN.getLogger().info(ChatColor.RED + "Disconnected from the Discord Bot!");
                PLUGIN.deleteConn();
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

    public static void checkVersion() {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(BOT_URI + "/version").openConnection();
            InputStream inputStream = conn.getInputStream();
            String latestVersion = new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining("\n"));
            if(!latestVersion.equals(PLUGIN_VERSION))
                DiscordLinker.getPlugin().getLogger().info(ChatColor.AQUA + "Please update to the latest Discord-Linker version (" + latestVersion + ") for a bug-free and feature-rich experience.");

        }
        catch(IOException ignored) {}
    }

    /**
     * Connects to the websocket server with a verification code.
     *
     * @param code     The verification code to connect with.
     * @param callback The callback to run when the connection is established or fails.
     */
    public void connectWebsocket(String code, Consumer<Boolean> callback) {
        //Create random 32-character hex string
        String token = new BigInteger(130, new SecureRandom()).toString(16);
        Map<String, String> auth = new HashMap<>();
        auth.put("code", code);
        auth.put("token", token);

        WebSocketConnection tempAdapter = new WebSocketConnection(auth);

        //Set listeners
        tempAdapter.getSocket().on("auth-success", data -> {
            //Code is valid, set the adapter to the new one
            this.socket = tempAdapter.getSocket();
            tempAdapter.socket = null;

            JsonObject dataObject = new JsonParser().parse(data[0].toString()).getAsJsonObject();

            //Save connection data
            JsonObject connJson = new JsonObject();
            connJson.addProperty("protocol", "websocket");
            connJson.addProperty("id", code.split(":")[0]);
            connJson.addProperty("token", token);
            connJson.add("channels", new JsonArray());
            connJson.add("synced-roles", new JsonArray());
            connJson.add("stats-channels", new JsonArray());
            if(dataObject.has("requiredRoleToJoin") && !dataObject.get("requiredRoleToJoin").isJsonNull())
                connJson.add("requiredRoleToJoin", dataObject.get("requiredRoleToJoin"));

            try {
                DiscordLinker.getPlugin().updateConn(connJson);
                callback.accept(true);
            }
            catch(IOException err) {
                DiscordLinker.getPlugin().getLogger().info(ChatColor.RED + "Failed to save connection data.");
                err.printStackTrace();

                disconnect();
                callback.accept(false);
            }

            tempAdapter.getSocket().off("auth-success"); // Only run once
        });
    }

    public void disconnectForce() {
        send("disconnect-force", new JsonObject());
        DiscordLinker.getPlugin().deleteConn();
    }

    public boolean isWebSocketConnected() {
        return this.getSocket().connected();
    }

    public void send(String event, JsonObject body) {
        if(isWebSocketConnected()) socket.emit(event, body);
    }

    public void send(String event, JsonObject body, Consumer<Router.RouterResponse> callback) {
        if(isWebSocketConnected()) socket.emit(event, body, new AckWithTimeout(5000) {
            @Override
            public void onSuccess(Object... args) {
                try {
                    callback.accept(new Router.RouterResponse(Status._200, args[0].toString()));
                }
                catch(Exception e) {
                    callback.accept(null);
                }
            }

            @Override
            public void onTimeout() {
                callback.accept(null);
            }
        });
    }

    public void chat(String message, ChatType type, String player) {
        JsonArray channels = DiscordLinker.getPlugin().filterChannels(type);
        if(channels == null || channels.size() == 0) return;

        JsonObject chatJson = new JsonObject();
        chatJson.addProperty("type", type.getKey());
        chatJson.addProperty("player", player);
        chatJson.addProperty("message", ChatColor.stripColor(message));
        chatJson.add("channels", channels);

        send("chat", chatJson);
    }

    public void updateStatsChannel(StatsUpdateEvent event) {
        JsonArray channels = DiscordLinker.getPlugin().filterChannels(event);
        if(channels == null || channels.size() == 0) return;

        JsonObject statsJson = new JsonObject();
        statsJson.addProperty("event", event.getName());
        statsJson.add("channels", channels);

        if(event == StatsUpdateEvent.MEMBERS) statsJson.addProperty("members", Bukkit.getOnlinePlayers().size());

        send("update-stats-channels", statsJson);
    }

    public void addSyncedRoleMember(String name, boolean isGroup, UUID uuid) {
        updateSyncedRoleMember(name, isGroup, uuid, "add");
    }

    public void removeSyncedRoleMember(String name, boolean isGroup, UUID uuid) {
        updateSyncedRoleMember(name, isGroup, uuid, "remove");
    }

    private void updateSyncedRoleMember(String name, boolean isGroup, UUID uuid, String addOrRemove) {
        getSyncedRole(name, isGroup, role -> {
            if(role == null) return;
            JsonObject payload = new JsonObject();
            payload.add("id", role.get("id"));
            payload.addProperty("uuid", uuid.toString());
            if(addOrRemove.equals("add"))
                send("add-synced-role-member", payload);
            else if(addOrRemove.equals("remove"))
                send("remove-synced-role-member", payload);
        });
    }

    public void removeSyncedRole(String name, boolean isGroup) {
        boolean hadTeamSyncedRole = DiscordLinker.getPlugin().hasTeamSyncedRole();
        getSyncedRole(name, isGroup, role -> {
            if(role == null) return;
            send("remove-synced-role", role);
            Router.handleChangeArray(role, "synced-roles", "remove");

            boolean hasTeamSyncedRole = DiscordLinker.getPlugin().hasTeamSyncedRole();
            if(hadTeamSyncedRole && !hasTeamSyncedRole) TeamChangeEvent.stopTeamCheck();
        });
    }

    public void getSyncedRole(String name, boolean isGroup, Consumer<JsonObject> callback) {
        if(DiscordLinker.getConnJson() == null) {
            callback.accept(null);
            return;
        }
        JsonArray syncedRoles = DiscordLinker.getConnJson().get("synced-roles").getAsJsonArray();

        // Check if the team/group is synced
        AtomicReference<JsonObject> role = new AtomicReference<>();
        for(JsonElement roleJson : syncedRoles) {
            JsonObject roleObj = roleJson.getAsJsonObject();
            if(roleObj.get("name").getAsString().equals(name) && roleObj.get("isGroup").getAsBoolean() == isGroup)
                role.set(roleObj);
        }
        if(role.get() == null) {
            callback.accept(null);
            return;
        }

        Router.getPlayers(name, isGroup, uuids -> {
            if(uuids == null) {
                callback.accept(null);
                return;
            }

            JsonArray players = new JsonArray();
            uuids.forEach(players::add);
            role.get().add("players", players);

            //Update connJson
            Router.handleChangeArray(role.get(), "synced-roles", "add");

            callback.accept(role.get());
        });
    }

    public void sendVerificationResponse(String code, UUID uuid) {
        JsonObject verifyJson = new JsonObject();
        verifyJson.addProperty("code", code);
        verifyJson.addProperty("uuid", uuid.toString());

        send("verify-response", verifyJson);
    }

    public void hasRequiredRole(UUID uuid, Consumer<HasRequiredRoleResponse> callback) {
        JsonObject verifyJson = new JsonObject();
        verifyJson.addProperty("uuid", uuid.toString());

        send("has-required-role", verifyJson, body -> {
            if(body == null) callback.accept(HasRequiredRoleResponse.ERROR);
            else {
                try {
                    HasRequiredRoleResponse response = HasRequiredRoleResponse.valueOf(body.getJson().getAsJsonObject().get("response").getAsString().toUpperCase());
                    callback.accept(response);
                }
                catch(Exception e) {
                    callback.accept(HasRequiredRoleResponse.ERROR);
                }
            }
        });
    }

    public void verifyUser(Player player, int code) {
        JsonObject verifyJson = new JsonObject();
        verifyJson.addProperty("code", String.valueOf(code));
        verifyJson.addProperty("uuid", player.getUniqueId().toString());
        verifyJson.addProperty("username", player.getName());

        send("verify-user", verifyJson);
    }

    public void getInviteURL(Consumer<String> callback) {
        send("invite-url", new JsonObject(), body -> {
            if(body == null) callback.accept(null);
            else {
                try {
                    JsonObject bodyObject = body.getJson().getAsJsonObject();
                    if(bodyObject.get("url").isJsonNull()) callback.accept(null);
                    else callback.accept(bodyObject.get("url").getAsString());
                }
                catch(Exception e) {
                    callback.accept(null);
                }
            }
        });
    }

    public void disconnect() {
        socket.disconnect();
    }

    public Socket getSocket() {
        return socket;
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

    public void connect(Consumer<Boolean> callback) {
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
}
