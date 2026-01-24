package me.lianecx.discordlinker.common.network.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.lianecx.discordlinker.common.ConnJson;
import me.lianecx.discordlinker.common.abstraction.LinkerOfflinePlayer;
import me.lianecx.discordlinker.common.abstraction.LinkerPlayer;
import me.lianecx.discordlinker.common.abstraction.LinkerServer;
import me.lianecx.discordlinker.common.network.protocol.events.LinkerDiscordEventBus;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventJsonResponse;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventResponse;
import me.lianecx.discordlinker.common.network.protocol.responses.HasRequiredRoleResponses;
import me.lianecx.discordlinker.common.util.JsonUtil;
import me.lianecx.discordlinker.common.util.MinecraftChatColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static me.lianecx.discordlinker.common.DiscordLinkerCommon.*;
import static me.lianecx.discordlinker.common.network.client.WebSocketDiscordClient.DEFAULT_RECONNECTION_ATTEMPTS;
import static me.lianecx.discordlinker.common.util.URLEncoderUtil.encodeURL;

public final class ClientManager {

    public static int DEFAULT_BOT_PORT = 80;

    //If snapshot version, request test-bot at port 81 otherwise request main-bot at port 80/config-port
    public static int BOT_PORT = getConfig().isTestVersion() ? 81 : getConfig().getBotPort();
    public static URI BOT_URI = URI.create("http://api.mclinker.com:" + BOT_PORT);

    private final LinkerDiscordEventBus linkerDiscordEventBus = new LinkerDiscordEventBus();

    private @Nullable DiscordClient client;

    public ClientManager() {}

    public ClientManager(String token) {
        this.client = new WebSocketDiscordClient(Collections.singletonMap("token", token), getConnectionQuery());
        client.onAny(linkerDiscordEventBus::emit);
    }

    public void setBotPort(int port) {
        BOT_PORT = port;
        BOT_URI = URI.create("http://api.mclinker.com:" + BOT_PORT);
    }

    public static void checkVersion() {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(BOT_URI + "/version").openConnection();
            try(InputStream inputStream = conn.getInputStream()) {
                String latestVersion = new BufferedReader(new InputStreamReader(inputStream)).readLine();
                if(!latestVersion.equals(getConfig().getPluginVersion()))
                    getLogger().info(MinecraftChatColor.AQUA + "Please update to the latest Discord-Linker version (" + latestVersion + ") for a bug-free and feature-rich experience.");
            }
            catch(IOException ignored) {}
        }
        catch(IOException e) {
            getLogger().warn(MinecraftChatColor.YELLOW + "Could not check for updates: " + e.getMessage());
        }
    }

    /**
     * Connects to the bot with the saved token passed in the constructor.
     * @param callback The callback to run when the connection is established or fails.
     */
    public void reconnect(Consumer<Boolean> callback) {
        if(client == null) {
            callback.accept(false);
            return;
        }
        client.disconnect();
        client.connect(callback);
    }

    /**
     * Connects to the bot with a verification code for the first time.
     * @param code     The verification code to connect with.
     * @param callback The callback to run when the connection is established or fails.
     */
    public void connectWithCode(String code, Consumer<Boolean> callback) {
        disconnect();

        //Create random 32-character hex string as token
        String token = new BigInteger(130, new SecureRandom()).toString(16);
        Map<String, String> auth = new HashMap<>();
        auth.put("code", code);
        auth.put("token", token);

        WebSocketDiscordClient tempClient = new WebSocketDiscordClient(auth, getConnectionQuery(), 5);

        //Set listeners
        tempClient.once("auth-success", data -> {
            //Code is valid, set the adapter to the new one
            tempClient.setReconnectionAttempts(DEFAULT_RECONNECTION_ATTEMPTS);
            client = tempClient;

            JsonObject dataObject = JsonUtil.getJsonObjectFromObjects(data);
            if(dataObject == null) {
                callback.accept(false);
                disconnect();
                return completedFuture(new DiscordEventJsonResponse(DiscordEventJsonResponse.JsonStatus.ERROR, "Invalid payload: " + Arrays.toString(data)));
            }

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

            boolean writeSuccess = ConnJson.update(connJson);
            callback.accept(writeSuccess);
            if(!writeSuccess) {
                disconnect();
                return completedFuture(DiscordEventJsonResponse.ERROR_WRITE_CONN);
            }

            return completedFuture(DiscordEventJsonResponse.SUCCESS);
        });

        tempClient.connect(connected -> {
            //If connected, the bot will call auth-success above
            if(connected) return;

            tempClient.disconnect();
            callback.accept(false);
            //Connect to old adapter if it exists
            reconnect(ignored -> {});
        });
    }

    public void chat(ConnJson.ChatChannel.ChatChannelType type) {
        chat("", type, null);
    }

    public void chat(String message, ConnJson.ChatChannel.ChatChannelType type, String player) {
        JsonObject payload = new JsonObject();
        payload.addProperty("message", message);
        payload.addProperty("type", type.toString());
        payload.addProperty("player", player);
        send("chat", payload);
    }

    public void updateStatsChannel(ConnJson.StatsChannel.StatsChannelEvent event) {
        send("update-stats-channels", JsonUtil.singleton("event", event.toString()));
    }

    public void addSyncedRoleMember(String name, boolean isGroup, UUID uuid) {
        updateSyncedRoleMember(name, isGroup, uuid, "add");
    }

    public void removeSyncedRoleMember(String name, boolean isGroup, UUID uuid) {
        updateSyncedRoleMember(name, isGroup, uuid, "remove");
    }

    private void updateSyncedRoleMember(String name, boolean isGroup, UUID uuid, String addOrRemove) {
        getSyncedRoleAndUpdatePlayers(name, isGroup, role -> {
            if(role == null) return;

            JsonObject payload = new JsonObject();
            payload.addProperty("id", role.getId());
            payload.addProperty("uuid", uuid.toString());
            if(addOrRemove.equals("add"))
                send("add-synced-role-member", payload);
            else if(addOrRemove.equals("remove"))
                send("remove-synced-role-member", payload);
        });
    }

    public void removeSyncedRole(String name, boolean isGroup) {
        if(getConnJson() == null || getConnJson().getSyncedRoles() == null) return;

        getSyncedRoleAndUpdatePlayers(name, isGroup, role -> {
            if(role == null) return;

            send("remove-synced-role", role);
            getConnJson().getSyncedRoles().remove(role);

            getTeamsAndGroupsBridge().stopTeamCheck(); // Stop if it was running before
        });
    }

    public void getSyncedRoleAndUpdatePlayers(String name, boolean isGroup, Consumer<ConnJson.SyncedRole> callback) {
        if(getConnJson() == null) {
            callback.accept(null);
            return;
        }

        ConnJson.SyncedRole role = getConnJson().getSyncedRole(name, isGroup);
        if(role == null) {
            callback.accept(null);
            return;
        }

        getTeamsAndGroupsBridge().getPlayersInGroupOrTeam(name, isGroup)
            .thenAccept(players -> {
                if(players == null) {
                    callback.accept(null);
                    return;
                }

                //Update players in role
                role.getPlayers().clear();
                for(LinkerOfflinePlayer player : players) {
                    role.getPlayers().add(player.getUUID());
                }

                callback.accept(role);
            });
    }

    /**
     * Acknowledges that a user has run `/account connect` and successfully verified their account.
     */
    public void sendVerificationResponse(String code, String uuid) {
        JsonObject verifyJson = new JsonObject();
        verifyJson.addProperty("code", code);
        verifyJson.addProperty("uuid", uuid);

        send("verify-response", verifyJson);
    }

    public void hasRequiredRole(String uuid, Consumer<HasRequiredRoleResponses> callback) {
        JsonObject verifyJson = new JsonObject();
        verifyJson.addProperty("uuid", uuid);

        send("has-required-role", verifyJson, response -> {
            if(!(response instanceof DiscordEventJsonResponse)) callback.accept(HasRequiredRoleResponses.ERROR);
            else {
                try {
                    String responseString = ((DiscordEventJsonResponse) response).getData().getAsJsonObject().get("response").getAsString();
                    HasRequiredRoleResponses roleResponse = HasRequiredRoleResponses.valueOf(responseString);
                    callback.accept(roleResponse);
                }
                catch(Exception e) {
                    callback.accept(HasRequiredRoleResponses.ERROR);
                }
            }
        });
    }

    /**
     * Tells the bot the verification code that has been shown to the user so it listens for their DM.
     */
    public void verifyUser(LinkerPlayer player, int code) {
        JsonObject verifyJson = new JsonObject();
        verifyJson.addProperty("code", String.valueOf(code));
        verifyJson.addProperty("uuid", player.getUUID());
        verifyJson.addProperty("username", player.getName());

        send("verify-user", verifyJson);
    }

    public void getInviteURL(Consumer<String> callback) {
        send("invite-url", new JsonObject(), response -> {
            if(!(response instanceof DiscordEventJsonResponse)) {
                callback.accept(null);
                return;
            }

            try {
                JsonObject bodyObject = ((DiscordEventJsonResponse) response).getData().getAsJsonObject();
                if(bodyObject.get("url").isJsonNull()) callback.accept(null);
                else callback.accept(bodyObject.get("url").getAsString());
            }
            catch(Exception e) {
                callback.accept(null);
            }
        });
    }

    /**
     * Sends an event to the bot with no data.
     */
    public void send(String eventName) {
        send(eventName, new Object[]{});
    }

    /**
     * Sends an event to the bot with a JSON object as data.
     */
    public void send(String eventName, JsonObject data) {
        send(eventName, data.toString());
    }

    /**
     * Sends an event to the bot with a JSON object as data and a callback for the response.
     */
    public void send(String eventName, JsonObject data, Consumer<DiscordEventResponse> callback) {
        if(client == null) return;
        client.send(eventName, new Object[] { data.toString() }, callback);
    }

    /**
     * Sends an event to the bot with arbitrary data.
     */
    public void send(String eventName, Object... data) {
        if(client == null) return;
        client.send(eventName, data);
    }

    public void disconnect() {
        if(client == null) return;
        client.disconnect();
    }

    public @NotNull Map<String, String> getConnectionQuery() {
        LinkerServer server = getServer();
        boolean isMinehut = server.isPluginOrModEnabled("MinehutPlugin");

        Map<String, String> response = new HashMap<>();
        response.put("version", server.getMinecraftVersion());
        // Minehut servers have online mode disabled in the server.properties file, because a proxy handles authentication
        response.put("online", String.valueOf(isMinehut || server.isOnline()));
        response.put("worldPath", encodeURL(server.getWorldPath()));
        response.put("path", encodeURL(server.getWorldContainerPath()));
        response.put("floodgatePrefix", server.getFloodgatePrefix());
        return response;
    }

    public void disconnectForce() {
        send("disconnect-force");
    }
}
