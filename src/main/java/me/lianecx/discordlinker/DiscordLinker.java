package me.lianecx.discordlinker;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import express.Express;
import io.socket.client.IO;
import io.socket.client.Socket;
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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;


public final class DiscordLinker extends JavaPlugin {

    private static final int PLUGIN_ID = 17143;
    private static JsonObject connJson;
    private static DiscordLinker plugin;
    private final FileConfiguration config = getConfig();

    private HttpAdapter httpAdapter;
    private WebSocketAdapter webSocketAdapter;


    @Override
    public void onEnable() {
        plugin = this;
        saveDefaultConfig();

        getServer().getPluginManager().registerEvents(new ChatListeners(), this);
        getCommand("linker").setExecutor(new LinkerCommand());
        getCommand("linker").setTabCompleter(new LinkerTabCompleter());
        getCommand("verify").setExecutor(new VerifyCommand());

        Router.init();
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            HttpConnection.checkVersion();

            try(Reader connReader = Files.newBufferedReader(Paths.get(getDataFolder() + "/connection.conn"))) {
                JsonElement parser = new JsonParser().parse(connReader);
                connJson = parser.isJsonObject() ? parser.getAsJsonObject() : null;

                HttpConnection.sendChat("", ChatType.START, null);
            }
            catch(IOException ignored) {}

            Metrics metrics = new Metrics(this, PLUGIN_ID);
            metrics.addCustomChart(new SimplePie("server_connected_with_discord", () -> connJson != null ? "true" : "false"));

            httpAdapter = startHttpServer();

            //Start websocket server if connection has been made previously
            if(connJson != null && connJson.get("protocol").getAsString().equals("websocket")) {
                webSocketAdapter = startSocketServer();
            }

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

    private WebSocketAdapter startSocketServer() {
        //Connect to bot's WebSocket server if plugin is connected to discord
        IO.Options ioOptions = IO.Options.builder()
                .setAuth(Collections.singletonMap("token", connJson.get("token").getAsString()))
                .setReconnectionDelayMax(10000)
                .build();

        Socket socket = IO.socket(HttpConnection.BOT_URL, ioOptions);

        socket.on(Socket.EVENT_CONNECT_ERROR, args -> getLogger().info(ChatColor.RED + "Could not reach the Discord Bot! Reconnecting..."));
        socket.on(Socket.EVENT_CONNECT, args -> getLogger().info(ChatColor.GREEN + "Connected to the Discord Bot!"));
        socket.on(Socket.EVENT_DISCONNECT, args -> {
            if(args[0].equals("io server disconnect")) {
                getLogger().info(ChatColor.RED + "Disconnected from the Discord Bot!");
                this.disconnect();
            }
        });

        WebSocketAdapter adapter = new WebSocketAdapter(socket);
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

    public boolean disconnect() {
        connJson = null;
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
