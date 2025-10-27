package me.lianecx.discordlinker.network.adapters;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import express.http.RequestMethod;
import express.utils.Status;
import io.socket.client.AckWithTimeout;
import me.lianecx.discordlinker.DiscordLinker;
import me.lianecx.discordlinker.events.TeamChangeEvent;
import me.lianecx.discordlinker.network.ChatType;
import me.lianecx.discordlinker.network.HasRequiredRoleResponse;
import me.lianecx.discordlinker.network.Router;
import me.lianecx.discordlinker.network.StatsUpdateEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class AdapterManager {

    //If snapshot version, request test-bot at port 81 otherwise request main-bot at port 80/config-port
    public static int BOT_PORT = DiscordLinker.getPluginVersion().contains("SNAPSHOT") ? 81 :
            DiscordLinker.getPlugin().getConfig().getInt("bot_port", 80);
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

    public static void setBotPort(int botPort) {
        AdapterManager.BOT_PORT = botPort;
    }

    public static URI getBotURI() {
        return URI.create("http://api.mclinker.com:" + BOT_PORT);
    }

    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    public void start(Consumer<Boolean> callback) {
        if(adapter == null) return;

        //Reconnect to websocket if it was connected before
        adapter.disconnect();
        adapter.connect(httpPort, callback);
    }

    public void stop() {
        if(adapter != null) adapter.disconnect();
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
     *
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

        WebSocketAdapter tempAdapter = new WebSocketAdapter(auth, 5);

        //Set listeners
        tempAdapter.getSocket().once("auth-success", data -> {
            //Code is valid, set the adapter to the new one
            tempAdapter.setReconnectionAttempts(WebSocketAdapter.DEFAULT_RECONNECTION_ATTEMPTS);
            adapter = tempAdapter;

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

                stop();
                startHttp();
                callback.accept(false);
            }
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
        if(adapter instanceof WebSocketAdapter && !((WebSocketAdapter) adapter).getSocket().connected()) {
            adapter.disconnect(); // If the WebSocket is not connected, it might be trying to reconnect, so we disconnect it to stop that.
        }
        else send(RequestMethod.GET, "/disconnect-force", "disconnect-force", new JsonObject());
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
        else if(isHttpConnected()) HttpAdapter.send(method, route, body);
    }

    public void send(RequestMethod method, String route, String event, JsonObject body, Consumer<Router.RouterResponse> callback) {
        if(isWebSocketConnected()) ((WebSocketAdapter) adapter).send(event, body, new AckWithTimeout(5000) {
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
        else {
            callback.accept(HttpAdapter.send(method, route, body));
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
                send(RequestMethod.POST, "/add-synced-role-member", "add-synced-role-member", payload);
            else if(addOrRemove.equals("remove"))
                send(RequestMethod.POST, "/remove-synced-role-member", "remove-synced-role-member", payload);
        });
    }

    public void removeSyncedRole(String name, boolean isGroup) {
        boolean hadTeamSyncedRole = DiscordLinker.getPlugin().hasTeamSyncedRole();
        getSyncedRole(name, isGroup, role -> {
            if(role == null) return;
            send(RequestMethod.POST, "/remove-synced-role", "remove-synced-role", role);
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

        send(RequestMethod.POST, "/verify/response", "verify-response", verifyJson);
    }

    public void hasRequiredRole(UUID uuid, Consumer<HasRequiredRoleResponse> callback) {
        JsonObject verifyJson = new JsonObject();
        verifyJson.addProperty("uuid", uuid.toString());

        send(RequestMethod.POST, "/has-required-role", "has-required-role", verifyJson, body -> {
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

        send(RequestMethod.POST, "/verify-user", "verify-user", verifyJson);
    }

    public void getInviteURL(Consumer<String> callback) {
        send(RequestMethod.POST, "/invite-url", "invite-url", new JsonObject(), body -> {
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
}
