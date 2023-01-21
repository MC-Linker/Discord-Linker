package me.lianecx.discordlinker;

import com.google.gson.*;
import express.Express;
import express.http.RequestMethod;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import me.lianecx.discordlinker.commands.LinkerCommand;
import me.lianecx.discordlinker.commands.LinkerTabCompleter;
import me.lianecx.discordlinker.commands.VerifyCommand;
import me.lianecx.discordlinker.events.ChatListeners;
import me.lianecx.discordlinker.network.ChatType;
import me.lianecx.discordlinker.network.HttpConnection;
import me.lianecx.discordlinker.network.Router;
import me.lianecx.discordlinker.network.adapters.HttpAdapter;
import me.lianecx.discordlinker.network.adapters.WebSocketAdapter;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;


public final class DiscordLinker extends JavaPlugin {

    private static final int PLUGIN_ID = 17143;
    private static JsonObject connJson;
    private static DiscordLinker plugin;
    private static HttpAdapter httpAdapter;
    private static WebSocketAdapter webSocketAdapter;

    private final FileConfiguration config = getConfig();


    @Override
    public void onEnable() {
        plugin = this;
        saveDefaultConfig();

        getServer().getPluginManager().registerEvents(new ChatListeners(), this);
        getCommand("linker").setExecutor(new LinkerCommand());
        getCommand("linker").setTabCompleter(new LinkerTabCompleter());
        getCommand("verify").setExecutor(new VerifyCommand());

        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            HttpConnection.checkVersion();

            try(Reader connReader = Files.newBufferedReader(Paths.get(getDataFolder() + "/connection.conn"))) {
                Router.init(); //Try-catch the init

                JsonElement parser = new JsonParser().parse(connReader);
                connJson = parser.isJsonObject() ? parser.getAsJsonObject() : null;
            }
            catch(IOException ignored) {}

            String protocol = connJson != null ? connJson.get("protocol").getAsString() : null;

            //Start websocket server if connection has been made previously
            if(connJson != null && protocol.equals("websocket")) startSocketClient();
            else startHttpServer();
            sendChat("", ChatType.START, null);


            Metrics metrics = new Metrics(this, PLUGIN_ID);
            metrics.addCustomChart(new SimplePie("server_connected_with_discord", () -> connJson != null ? "true" : "false"));
            if(connJson != null) {
                //TODO register on https://bstats.org/plugin/bukkit/DiscordLinker/17143
                metrics.addCustomChart(new SimplePie("server_has_websocket", () -> protocol.equals("websocket") ? "true" : "false"));
                metrics.addCustomChart(new SimplePie("server_has_http", () -> protocol.equals("http") ? "true" : "false"));
            }
            else {
                metrics.addCustomChart(new SimplePie("server_has_websocket", () -> "false"));
                metrics.addCustomChart(new SimplePie("server_has_http", () -> "false"));
            }

            getLogger().info(ChatColor.GREEN + "Plugin enabled.");
        });
    }

    @Override
    public void onDisable() {
        sendChat("", ChatType.CLOSE, null);

        getServer().getScheduler().cancelTasks(this);
        closeHttpServer();
        closeSocketClient();

        getLogger().info(ChatColor.RED + "Plugin disabled.");
    }

    public static JsonObject getConnJson() {
        return connJson;
    }

    public static DiscordLinker getPlugin() {
        return plugin;
    }

    public void startSocketClient() {
        webSocketAdapter = new WebSocketAdapter(Collections.singletonMap("token", connJson.get("token").getAsString()));
        webSocketAdapter.connect();
    }

    public void startHttpServer(int port) {
        if(httpAdapter != null) {
            httpAdapter.disconnect();
            httpAdapter.connect(port);
        }

        Express app = new Express();
        httpAdapter = new HttpAdapter(app);

        httpAdapter.connect(port);
    }

    public void startHttpServer() {
        startHttpServer(config.getInt("port") != 0 ? config.getInt("port") : 11111);
    }

    public void closeHttpServer() {
        if(httpAdapter != null) httpAdapter.disconnect();
        httpAdapter = null;
    }

    public void closeSocketClient() {
        if(webSocketAdapter != null) webSocketAdapter.disconnect();
        webSocketAdapter = null;
    }

    /**
     * Connects to the websocket server with a verification code
     */
    public void connectWebsocketClient(String code, Consumer<Boolean> callback) {
        //Create random 32-character hex string
        String token = new BigInteger(130, new SecureRandom()).toString(16);

        Map<String, String> auth = new HashMap<>();
        auth.put("code", code);
        auth.put("token", token);
        if(webSocketAdapter != null) webSocketAdapter.disconnect();
        WebSocketAdapter tempAdapter = new WebSocketAdapter(auth);

        //Register connection listeners
        AtomicReference<Emitter.Listener> failListener = new AtomicReference<>();
        AtomicReference<Emitter.Listener> successListener = new AtomicReference<>();
        failListener.set(objects -> {
            tempAdapter.getSocket().off(Socket.EVENT_DISCONNECT, failListener.get());
            tempAdapter.getSocket().off(Socket.EVENT_CONNECT_ERROR, failListener.get());
            tempAdapter.getSocket().off(Socket.EVENT_CONNECT, successListener.get());
            callback.accept(false);
        });
        successListener.set(objects -> {
            tempAdapter.getSocket().off(Socket.EVENT_DISCONNECT, failListener.get());
            tempAdapter.getSocket().off(Socket.EVENT_CONNECT_ERROR, failListener.get());
            tempAdapter.getSocket().off(Socket.EVENT_CONNECT, successListener.get());

            webSocketAdapter = tempAdapter; //Code is valid, set the adapter to the new one

            //Save connection data
            connJson = new JsonObject();
            connJson.addProperty("protocol", "websocket");
            connJson.addProperty("token", token);
            connJson.add("channels", new JsonArray());
            connJson.addProperty("id", code.split(":")[0]);

            try {
                updateConn();
                callback.accept(true);
            }
            catch(IOException e) {
                webSocketAdapter.disconnect();
                getLogger().info(ChatColor.RED + "Failed to save connection data.");
                e.printStackTrace();
                callback.accept(false);
            }
        });

        tempAdapter.getSocket().on(Socket.EVENT_DISCONNECT, failListener.get());
        tempAdapter.getSocket().on(Socket.EVENT_CONNECT_ERROR, failListener.get());
        tempAdapter.getSocket().on("auth-success", successListener.get());

        tempAdapter.connect();
    }

    public boolean disconnect() {
        connJson = null;
        if(webSocketAdapter != null) webSocketAdapter = null;
        File connection = new File(getDataFolder() + "/connection.conn");
        return connection.delete();
    }

    public boolean isWebSocketConnected() {
        return webSocketAdapter != null && webSocketAdapter.getSocket().connected();
    }

    public void updateConn() throws IOException {
        FileWriter writer = new FileWriter(getDataFolder() + "/connection.conn");
        writer.write(connJson.toString());
        writer.close();
    }

    public void updateConn(JsonObject connJson) throws IOException {
        DiscordLinker.connJson = connJson;
        updateConn();
    }

    public void sendChat(String message, ChatType type, String player) {
        JsonArray channels = filterChannels(type);
        if(channels == null || channels.size() == 0) return;

        JsonObject chatJson = new JsonObject();
        chatJson.addProperty("type", type.getKey());
        chatJson.addProperty("player", player);
        chatJson.addProperty("message", ChatColor.stripColor(message));
        chatJson.add("channels", channels);
        chatJson.add("id", DiscordLinker.getConnJson().get("id"));

        if(connJson.get("protocol").getAsString().equals("websocket")) {
            webSocketAdapter.send("chat", chatJson);
        }
        else {
            int code = HttpConnection.send(RequestMethod.POST, "/chat", chatJson);
            if(code == 403) DiscordLinker.getPlugin().disconnect();
        }
    }

    private boolean shouldChat() {
        if(connJson == null || connJson.get("channels") == null) return false;
        return connJson.getAsJsonArray("channels").size() > 0;
    }

    private JsonArray filterChannels(ChatType type) {
        if(!shouldChat()) return null;

        JsonArray allChannels = connJson.getAsJsonArray("channels");
        JsonArray filteredChannels = new JsonArray();
        for(JsonElement channel : allChannels) {
            try {
                JsonArray types = channel.getAsJsonObject().getAsJsonArray("types");
                if(types.contains(new JsonPrimitive(type.getKey()))) filteredChannels.add(channel);
            }
            catch(Exception err) {
                //If channel is corrupted, remove
                allChannels.remove(channel);

                try {
                    updateConn();
                }
                catch(IOException ignored) {}
            }
        }

        return filteredChannels;
    }
}
