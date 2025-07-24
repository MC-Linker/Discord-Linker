package me.lianecx.discordlinker;

import com.google.gson.*;
import me.lianecx.discordlinker.commands.DiscordCommand;
import me.lianecx.discordlinker.commands.LinkerCommand;
import me.lianecx.discordlinker.commands.LinkerTabCompleter;
import me.lianecx.discordlinker.commands.VerifyCommand;
import me.lianecx.discordlinker.events.ChatListeners;
import me.lianecx.discordlinker.events.JoinEvent;
import me.lianecx.discordlinker.events.TeamChangeEvent;
import me.lianecx.discordlinker.network.ChatType;
import me.lianecx.discordlinker.network.Router;
import me.lianecx.discordlinker.network.StatsUpdateEvent;
import me.lianecx.discordlinker.network.adapters.AdapterManager;
import me.lianecx.discordlinker.network.adapters.HttpAdapter;
import me.lianecx.discordlinker.utilities.LuckPermsUtil;
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

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static String getPluginVersion() {
        return DiscordLinker.getPlugin().getDescription().getVersion();
    }

    public static JsonObject getConnJson() {
        return connJson;
    }

    public static DiscordLinker getPlugin() {
        return plugin;
    }

    public static AdapterManager getAdapterManager() {
        return adapterManager;
    }

    public static Gson getGson() {
        return gson;
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
            if(protocol != null) {
                if(protocol.equals("websocket")) {
                    String token = connJson.get("token").getAsString();

                    adapterManager = new AdapterManager(token, getPort());
                    adapterManager.start(connected -> {
                        if(!connected) return;
                        adapterManager.chat("", ChatType.START, null);
                        adapterManager.updateStatsChannel(StatsUpdateEvent.ONLINE);
                    });
                }
                else {
                    getLogger().warning(ChatColor.GOLD + "**Your server is using the deprecated backup connection method and will be disconnected. Please reconnect in Discord using `/connect`.**");
                    adapterManager.disconnectForce();
                }
            }
            else adapterManager = new AdapterManager(getPort());

            Metrics metrics = new Metrics(this, PLUGIN_ID);
            metrics.addCustomChart(new SimplePie("server_connected_with_discord", () -> connJson != null ? "true" : "false"));
            metrics.addCustomChart(new SimplePie("server_uses_http", () -> Objects.equals(protocol, "http") ? "true" : "false"));

            getServer().getPluginManager().registerEvents(new ChatListeners(), this);
            getServer().getPluginManager().registerEvents(new JoinEvent(), this);
            getServer().getPluginManager().registerEvents(new TeamChangeEvent(), this);
            getCommand("linker").setExecutor(new LinkerCommand());
            getCommand("linker").setTabCompleter(new LinkerTabCompleter());
            getCommand("verify").setExecutor(new VerifyCommand());
            getCommand("discord").setExecutor(new DiscordCommand());

            if(hasTeamSyncedRole())
                TeamChangeEvent.startTeamCheck();
            if(getServer().getPluginManager().isPluginEnabled("LuckPerms")) LuckPermsUtil.init();

            getLogger().info(ChatColor.GREEN + "Plugin enabled.");
        });
    }

    @Override
    public void onDisable() {
        adapterManager.chat("", ChatType.CLOSE, null);
        adapterManager.updateStatsChannel(StatsUpdateEvent.OFFLINE);
        adapterManager.updateStatsChannel(StatsUpdateEvent.MEMBERS);
        adapterManager.stop();

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
                err.printStackTrace();
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

    public boolean hasTeamSyncedRole() {
        if(connJson == null || !connJson.has("synced-roles")) return false;

        for(JsonElement syncedRole : connJson.getAsJsonArray("synced-roles")) {
            if(!syncedRole.getAsJsonObject().get("isGroup").getAsBoolean()) return true;
        }

        return false;
    }
}
