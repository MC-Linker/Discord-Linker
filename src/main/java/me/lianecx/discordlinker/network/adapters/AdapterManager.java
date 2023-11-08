package me.lianecx.discordlinker.network.adapters;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import express.http.RequestMethod;
import io.socket.client.AckWithTimeout;
import me.lianecx.discordlinker.DiscordLinker;
import me.lianecx.discordlinker.network.ChatType;
import me.lianecx.discordlinker.network.HasRequiredRoleResponse;
import me.lianecx.discordlinker.network.Router;
import me.lianecx.discordlinker.network.StatsUpdateEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class AdapterManager {

    private int httpPort;

    private NetworkAdapter adapter;

    public AdapterManager(String token, int httpPort) {
        this.httpPort = httpPort;
        adapter = new WebSocketAdapter(Collections.singletonMap("token", token));
    }

    public AdapterManager(int httpPort) {
        this.httpPort = httpPort;
        adapter = new HttpAdapter();
    }

    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    public void start(Consumer<Boolean> callback) {
        //Reconnect to websocket if it was connected before
        adapter.disconnect();
        adapter.connect(httpPort, callback);
    }

    public void stop() {
        // This throws an error on reload and server stop, probably because spigot already started unloading some classes when this gets called
        adapter.disconnect();
    }

    public void startHttp() {
        stop();
        adapter = new HttpAdapter();
        start(bool -> {});
    }

    public void stopHttp() {
        if(isHttpConnected()) {
            adapter.disconnect();
            adapter = null;
        }
    }

    /**
     * Connects to the websocket server with a verification code.
     * @param code     The verification code to connect with.
     * @param callback The callback to run when the connection is established or fails.
     */
    public void connectWebsocket(String code, Consumer<Boolean> callback) {
        stop();

        //Create random 32-character hex string
        String token = new BigInteger(130, new SecureRandom()).toString(16);
        Map<String, String> auth = new HashMap<>();
        auth.put("code", code);
        auth.put("token", token);

        WebSocketAdapter tempAdapter = new WebSocketAdapter(auth);

        //Set listeners
        tempAdapter.getSocket().on("auth-success", data -> {
            //Code is valid, set the adapter to the new one
            adapter = tempAdapter;

            JsonObject dataObject = new JsonParser().parse(data[0].toString()).getAsJsonObject();

            //Save connection data
            JsonObject connJson = new JsonObject();
            connJson.addProperty("protocol", "websocket");
            connJson.addProperty("id", code.split(":")[0]);
            connJson.addProperty("token", token);
            connJson.add("channels", new JsonArray());
            if(dataObject.has("requiredRoleToJoin") && !dataObject.get("requiredRoleToJoin").isJsonNull())
                connJson.add("requiredRoleToJoin", dataObject.get("requiredRoleToJoin"));

            try {
                DiscordLinker.getPlugin().updateConn(connJson);
                callback.accept(true);
            }
            catch(IOException err) {
                DiscordLinker.getPlugin().getLogger().info(ChatColor.RED + "Failed to save connection data.");
                err.printStackTrace();

                stop();
                startHttp();
                callback.accept(false);
            }

            tempAdapter.getSocket().off("auth-success"); // Only run once
        });

        tempAdapter.connect(httpPort, connected -> {
            //If connected, the bot will call auth-success above
            if(connected) return;

            tempAdapter.disconnect();
            callback.accept(false);
            //Connect to old adapter if it exists
            if(adapter != null) adapter.connect(httpPort, bool -> {});
        });
    }

    public void disconnectForce() {
        send(RequestMethod.GET, "/disconnect-force", "disconnect-force", new JsonObject());
        DiscordLinker.getPlugin().deleteConn();
    }

    public boolean isWebSocketConnected() {
        return adapter instanceof WebSocketAdapter && ((WebSocketAdapter) adapter).getSocket().connected();
    }

    public boolean isHttpConnected() {
        return adapter instanceof HttpAdapter;
    }

    public void send(RequestMethod method, String route, String event, JsonObject body) {
        if(isWebSocketConnected()) ((WebSocketAdapter) adapter).send(event, body);
        else HttpAdapter.send(method, route, body);
    }

    public void send(RequestMethod method, String route, String event, JsonObject body, BiConsumer<Boolean, JsonObject> callback) {
        if(isWebSocketConnected()) ((WebSocketAdapter) adapter).send(event, body, new AckWithTimeout(5000) {
            @Override
            public void onSuccess(Object... args) {
                try {
                    JsonObject body = new JsonParser().parse(args[0].toString()).getAsJsonObject();
                    callback.accept(false, body);
                }
                catch(Exception e) {
                    callback.accept(false, null);
                }
            }

            @Override
            public void onTimeout() {
                callback.accept(true, null);
            }
        });
        else {
            HttpAdapter.HttpResponse httpResponse = HttpAdapter.send(method, route, body);
            callback.accept(httpResponse == null, httpResponse == null ? null : httpResponse.getBody());
        }
    }

    public void chat(String message, ChatType type, String player) {
        JsonArray channels = DiscordLinker.getPlugin().filterChannels(type);
        if(channels == null || channels.size() == 0) return;

        JsonObject chatJson = new JsonObject();
        chatJson.addProperty("type", type.getKey());
        chatJson.addProperty("player", player);
        chatJson.addProperty("message", ChatColor.stripColor(message));
        chatJson.add("channels", channels);

        send(RequestMethod.POST, "/chat", "chat", chatJson);
    }

    public void updateStatsChannel(StatsUpdateEvent event) {
        JsonArray channels = DiscordLinker.getPlugin().filterChannels(event);
        if(channels == null || channels.size() == 0) return;

        JsonObject statsJson = new JsonObject();
        statsJson.addProperty("event", event.getName());
        statsJson.add("channels", channels);

        if(event == StatsUpdateEvent.MEMBERS) statsJson.addProperty("members", Bukkit.getOnlinePlayers().size());

        send(RequestMethod.POST, "/update-stats-channels", "update-stats-channels", statsJson);
    }

    public void updateSyncedRole(String name, boolean isGroup) {
        getSyncedRole(name, isGroup, true, role -> {
            if(role == null) return;
            send(RequestMethod.POST, "/update-synced-role", "update-synced-role", role);
        });
    }

    public void removeSyncedRole(String name, boolean isGroup) {
        getSyncedRole(name, isGroup, false, role -> {
            if(role == null) return;
            send(RequestMethod.POST, "/remove-synced-role", "remove-synced-role", role);
        });
    }

    private void getSyncedRole(String name, boolean isGroup, boolean addEntry, Consumer<JsonObject> callback) {
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
            JsonArray players = new JsonArray();
            uuids.forEach(players::add);
            role.get().add("players", players);

            //Update connJson
            Router.handleChangeArray(role.get(), "synced-roles", addEntry);

            callback.accept(role.get());
        });
    }

    public void sendVerificationResponse(String code, UUID uuid) {
        JsonObject verifyJson = new JsonObject();
        verifyJson.addProperty("code", code);
        verifyJson.addProperty("uuid", uuid.toString());

        send(RequestMethod.POST, "/verify/response", "verify-response", verifyJson);
    }

    public void hasRequiredRole(UUID uuid, Consumer<HasRequiredRoleResponse> callback) {
        JsonObject verifyJson = new JsonObject();
        verifyJson.addProperty("uuid", uuid.toString());

        send(RequestMethod.POST, "/has-required-role", "has-required-role", verifyJson, (timeout, body) -> {
            if(timeout) callback.accept(HasRequiredRoleResponse.ERROR);
            else {
                try {
                    HasRequiredRoleResponse response = HasRequiredRoleResponse.valueOf(body.get("response").getAsString().toUpperCase());
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

        send(RequestMethod.POST, "/verify-user", "verify-user", verifyJson);
    }

    public void getInviteURL(Consumer<String> callback) {
        send(RequestMethod.POST, "/invite-url", "invite-url", new JsonObject(), (timeout, body) -> {
            if(timeout) callback.accept(null);
            else {
                try {
                    if(body.get("url").isJsonNull()) callback.accept(null);
                    else callback.accept(body.get("url").getAsString());
                }
                catch(Exception e) {
                    callback.accept(null);
                }
            }
        });
    }
}
