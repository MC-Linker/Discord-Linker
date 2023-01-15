package me.lianecx.discordlinker;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import express.Express;
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

                HttpConnection.sendChat("", ChatType.START, null);
            }
            catch(IOException ignored) {}

            Metrics metrics = new Metrics(this, PLUGIN_ID);
            metrics.addCustomChart(new SimplePie("server_connected_with_discord", () -> connJson != null ? "true" : "false"));
            if(connJson != null) {
                //TODO register on https://bstats.org/plugin/bukkit/DiscordLinker/17143
                metrics.addCustomChart(new SimplePie("server_has_websocket", () -> connJson.get("protocol").getAsString().equals("websocket") ? "true" : "false"));
                metrics.addCustomChart(new SimplePie("server_has_http", () -> connJson.get("protocol").getAsString().equals("http") ? "true" : "false"));
            }
            metrics.addCustomChart(new SimplePie("server_has_websocket", () -> "false"));
            metrics.addCustomChart(new SimplePie("server_has_http", () -> "true"));

            //Start websocket server if connection has been made previously
            if(connJson != null && connJson.get("protocol").getAsString().equals("websocket")) {
                webSocketAdapter = startSocketClient();
            }
            else httpAdapter = startHttpServer();

            getLogger().info(ChatColor.GREEN + "Plugin enabled.");
        });
    }

    @Override
    public void onDisable() {
        HttpConnection.sendChat("", ChatType.CLOSE, null);
        getServer().getScheduler().cancelTasks(this);
        httpAdapter.disconnect();
        if(webSocketAdapter != null) webSocketAdapter.disconnect();

        getLogger().info(ChatColor.RED + "Plugin disabled.");
    }

    public static JsonObject getConnJson() {
        return connJson;
    }

    public static DiscordLinker getPlugin() {
        return plugin;
    }

    private WebSocketAdapter startSocketClient() {
        WebSocketAdapter adapter = new WebSocketAdapter(Collections.singletonMap("token", connJson.get("token").getAsString()));
        adapter.connect();

        return adapter;
    }

    private HttpAdapter startHttpServer() {
        Express app = new Express();
        HttpAdapter adapter = new HttpAdapter(app);

        int port = config.getInt("port") != 0 ? config.getInt("port") : 11111;
        adapter.connect(port);

        return adapter;
    }

    public void restartHttpServer(int port) {
        httpAdapter.disconnect();
        httpAdapter.connect(port);
    }

    //Connects to the websocket server with a verification code
    public void connectWebsocketClient(String code) {
        //Create random 32-character hex string
        String token = new BigInteger(130, new SecureRandom()).toString(16);

        Map<String, String> auth = new HashMap<>();
        auth.put("code", code);
        auth.put("token", token);
        webSocketAdapter = new WebSocketAdapter(auth);
        webSocketAdapter.connect();

        //Save connection data
        connJson = new JsonObject();
        connJson.addProperty("protocol", "websocket");
        connJson.addProperty("token", token);
        connJson.add("channels", new JsonArray());

        try {
            updateConn();
        }
        catch(IOException e) {
            webSocketAdapter.disconnect();
            getLogger().info(ChatColor.RED + "Failed to save connection data. Please try again.");
        }
    }

    public boolean disconnect() {
        connJson = null;
        if(webSocketAdapter != null) webSocketAdapter = null;
        File connection = new File(getDataFolder() + "/connection.conn");
        return connection.delete();
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
}
