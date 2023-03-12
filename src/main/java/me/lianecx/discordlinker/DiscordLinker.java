package me.lianecx.discordlinker;

import com.google.gson.*;
import me.lianecx.discordlinker.commands.LinkerCommand;
import me.lianecx.discordlinker.commands.LinkerTabCompleter;
import me.lianecx.discordlinker.commands.VerifyCommand;
import me.lianecx.discordlinker.events.ChatListeners;
import me.lianecx.discordlinker.network.ChatType;
import me.lianecx.discordlinker.network.Router;
import me.lianecx.discordlinker.network.StatsUpdateEvent;
import me.lianecx.discordlinker.network.adapters.AdapterManager;
import me.lianecx.discordlinker.network.adapters.HttpAdapter;
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
import java.util.Objects;


public final class DiscordLinker extends JavaPlugin {

    private static final int PLUGIN_ID = 17143;
    private static JsonObject connJson;
    private static DiscordLinker plugin;
    private static AdapterManager adapterManager;

    private final FileConfiguration config = getConfig();


    public static JsonObject getConnJson() {
        return connJson;
    }

    public static DiscordLinker getPlugin() {
        return plugin;
    }

    public static AdapterManager getAdapterManager() {
        return adapterManager;
    }

    public int getPort() {
        return config.getInt("port") != 0 ? config.getInt("port") : 11111;
    }

    @Override
    public void onEnable() {
        plugin = this;
        saveDefaultConfig();

        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            HttpAdapter.checkVersion();

            try(Reader connReader = Files.newBufferedReader(Paths.get(getDataFolder() + "/connection.conn"))) {
                Router.init(); //Try-catch the init

                JsonElement parser = new JsonParser().parse(connReader);
                connJson = parser.isJsonObject() ? parser.getAsJsonObject() : null;
            }
            catch(IOException ignored) {}

            String protocol = connJson != null && connJson.get("protocol") != null ? connJson.get("protocol").getAsString() : null;
            String token = Objects.equals(protocol, "websocket") ? connJson.get("token").getAsString() : null;

            if(Objects.equals(protocol, "websocket")) adapterManager = new AdapterManager(token, getPort());
            else adapterManager = new AdapterManager(getPort());
            adapterManager.startAll(connected -> {
                if(!connected) return;
                adapterManager.sendChat("", ChatType.START, null);
                adapterManager.sendStatsUpdate(StatsUpdateEvent.ONLINE);
            });

            Metrics metrics = new Metrics(this, PLUGIN_ID);
            metrics.addCustomChart(new SimplePie("server_connected_with_discord", () -> connJson != null ? "true" : "false"));
            metrics.addCustomChart(new SimplePie("server_uses_http", () -> Objects.equals(protocol, "http") ? "true" : "false"));

            getServer().getPluginManager().registerEvents(new ChatListeners(), this);
            getCommand("linker").setExecutor(new LinkerCommand());
            getCommand("linker").setTabCompleter(new LinkerTabCompleter());
            getCommand("verify").setExecutor(new VerifyCommand());

            getLogger().info(ChatColor.GREEN + "Plugin enabled.");
        });
    }

    @Override
    public void onDisable() {
        adapterManager.sendChat("", ChatType.CLOSE, null);
        adapterManager.sendStatsUpdate(StatsUpdateEvent.OFFLINE);
        adapterManager.sendStatsUpdate(StatsUpdateEvent.MEMBERS);
        adapterManager.stopAll();

        getServer().getScheduler().cancelTasks(this);
        getLogger().info(ChatColor.RED + "Plugin disabled.");
    }

    public boolean deleteConn() {
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

    public boolean shouldChat() {
        if(connJson == null || connJson.get("channels") == null) return false;
        return connJson.getAsJsonArray("channels").size() > 0;
    }

    public JsonArray filterChannels(ChatType type) {
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

    public boolean shouldSendStats() {
        if(connJson == null || connJson.get("stats-channels") == null) return false;
        return connJson.getAsJsonArray("stats-channels").size() > 0;
    }

    public JsonArray filterChannels(StatsUpdateEvent type) {
        if(!shouldSendStats()) return null;

        JsonArray allChannels = connJson.getAsJsonArray("stats-channels");
        JsonArray filteredChannels = new JsonArray();
        for(JsonElement channel : allChannels) {
            if(channel.getAsJsonObject().get("type").getAsString().equals(type.getJsonKey())) {
                filteredChannels.add(channel);
            }
        }

        return filteredChannels;
    }
}
