package me.lianecx.discordlinker.common.network.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.lianecx.discordlinker.common.ConnJson;
import me.lianecx.discordlinker.common.abstraction.LinkerServer;
import me.lianecx.discordlinker.common.network.protocol.events.LinkerDiscordEvents;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventJsonResponse;
import me.lianecx.discordlinker.common.util.JsonUtil;
import me.lianecx.discordlinker.common.util.MinecraftChatColor;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.*;
import static me.lianecx.discordlinker.common.network.client.WebSocketDiscordClient.DEFAULT_RECONNECTION_ATTEMPTS;

public final class ClientManager {

    //If snapshot version, request test-bot at port 81 otherwise request main-bot at port 80/config-port
    public static int BOT_PORT = getConfig().isTestVersion() ? 81 : getConfig().getBotPort();
    public static URI BOT_URI = URI.create("http://api.mclinker.com:" + BOT_PORT);

    private final LinkerDiscordEvents linkerDiscordEvents = new LinkerDiscordEvents();

    private DiscordClient client;

    public ClientManager() {}

    public ClientManager(String token) {
        this.client = new WebSocketDiscordClient(Collections.singletonMap("token", token), getConnectionQuery());
        client.onAny(linkerDiscordEvents::handleDiscordEvent);
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

            boolean writeSuccess = updateConn(connJson);
            callback.accept(writeSuccess);
            if(!writeSuccess) {
                disconnect();
                return DiscordEventJsonResponse.ERROR_WRITE_CONN;
            }

            return DiscordEventJsonResponse.SUCCESS;
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

    public void updateStatsChannel(ConnJson.StatsChannel.StatChannelEvent event) {
        send("update-stats-channels", JsonUtil.singleton("event", event.toString()));
    }

    public void send(String eventName) {
        send(eventName, new Object[]{});
    }

    public void send(String eventName, JsonObject data) {
        send(eventName, data.toString());
    }

    public void send(String eventName, Object... data) {
        client.send(eventName, data);
    }

    public void disconnect() {
        client.disconnect();
    }

    public @NotNull Map<String, String> getConnectionQuery() {
        try {
            LinkerServer server = getServer();
            boolean isMinehut = server.isPluginOrModEnabled("MinehutPlugin");

            Map<String, String> response = new HashMap<>();
            response.put("version", server.getMinecraftVersion());
            // Minehut servers have online mode disabled in the server.properties file, because a proxy handles authentication
            response.put("online", String.valueOf(isMinehut || server.isOnline()));
            response.put("worldPath", URLEncoder.encode(server.getWorldPath(), "utf-8"));
            response.put("path", URLEncoder.encode(server.getWorldContainerPath(), "utf-8"));
            response.put("floodgatePrefix", server.getFloodgatePrefix());
            return response;
        }
        catch(IOException err) {
            // never happens (UTF-8 is always supported)
            return null;
        }
    }

    public void disconnectForce() {
        send("disconnect-force");
    }
}
