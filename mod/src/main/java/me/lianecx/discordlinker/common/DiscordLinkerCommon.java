package me.lianecx.discordlinker.common;

import com.google.gson.*;
import me.lianecx.discordlinker.common.abstraction.ConnJson;
import me.lianecx.discordlinker.common.abstraction.LinkerServer;
import me.lianecx.discordlinker.common.abstraction.MinecraftChatColor;
import me.lianecx.discordlinker.common.abstraction.core.LinkerConfig;
import me.lianecx.discordlinker.common.abstraction.core.LinkerLogger;
import org.jetbrains.annotations.Nullable;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DiscordLinkerCommon {

    public static final int DEFAULT_BOT_PORT = 80;
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final TypeAdapter<JsonElement> strictAdapter = new Gson().getAdapter(JsonElement.class);
    private static DiscordLinkerCommon discordLinker;
    private final LinkerConfig config;
    private final ClientManager clientManager;
    private final LinkerLogger logger;
    private final LinkerServer server;
    private ConnJson connJson;

    private DiscordLinkerCommon(LinkerLogger logger, LinkerConfig config, LinkerServer server) {
        this.logger = logger;
        this.config = config;
        this.server = server;
        this.connJson = loadConn();

        String token = connJson != null ? connJson.getToken() : null;
        this.clientManager = new ClientManager(token);

        ClientManager.checkVersion();
        connectToBot();

        logger.info(MinecraftChatColor.GREEN + "Discord-Linker enabled.");
    }

    public static synchronized DiscordLinkerCommon init(LinkerLogger logger, LinkerConfig config, LinkerServer server) {
        if (discordLinker != null) throw new IllegalStateException("DiscordLinkerCommon already initialized!");
        discordLinker = new DiscordLinkerCommon(logger, config, server);
        return discordLinker;
    }

    public static DiscordLinkerCommon getInstance() {
        if(discordLinker == null) throw new IllegalStateException("DiscordLinkerCommon has not been initialized yet!");
        return discordLinker;
    }

    public static void shutdown() {
        getClientManager().chat("", ConnJson.ChatChannel.ChatChannelType.CLOSE, null);
        getClientManager().updateStatsChannel(ConnJson.StatsChannel.StatChannelEvent.OFFLINE);
        getClientManager().updateStatsChannel(ConnJson.StatsChannel.StatChannelEvent.MEMBERS);
        getClientManager().stop();

        getLogger().info(MinecraftChatColor.RED + "Discord-Linker disabled.");
        discordLinker = null;
    }

    public static boolean isValidJson(String json) {
        try {
            strictAdapter.fromJson(json);
        } catch (JsonSyntaxException | IOException e) {
            return false;
        }
        return true;
    }

    public static boolean deleteConn() {
        discordLinker.connJson = null;

        Path connJsonPath = Paths.get(getServer().getDataFolder() + "/connection.conn");
        if(!Files.exists(connJsonPath)) return true;

        try {
            Files.delete(connJsonPath);
            return true;
        }
        catch(IOException e) {
            DiscordLinkerCommon.getLogger().error("Failed to delete connection data file!");
            e.printStackTrace();
            return false;
        }
    }

    public static void writeConn() {
        try {
            FileWriter writer = new FileWriter(getServer().getDataFolder() + "/connection.conn");
            writer.write(GSON.toJson(getConnJson()));
            writer.close();
        }
        catch(IOException e) {
            DiscordLinkerCommon.getLogger().error("Failed to write connection data to file!");
            e.printStackTrace();
        }
    }

    public static void updateConn(JsonObject connJson) throws IOException {
        getInstance().connJson = DiscordLinkerCommon.GSON.fromJson(connJson, ConnJson.class);
        writeConn();
    }

    public static LinkerLogger getLogger() {
        return getInstance().logger;
    }

    public static LinkerConfig getConfig() {
        return getInstance().config;
    }

    public static @Nullable ConnJson getConnJson() {
        return getInstance().connJson;
    }

    public static ClientManager getClientManager() {
        return getInstance().clientManager;
    }

    public static LinkerServer getServer() {
        return getInstance().server;
    }

    private void connectToBot() {
        ConnJson.ConnProtocol protocol = connJson != null ? connJson.getProtocol() : null;
        if(protocol == null)
            logger.warn(MinecraftChatColor.YELLOW + "No Discord server connected! Please invite the \"MC-Linker\" Discord-Bot (https://discord.com/discovery/applications/712759741528408064) and run `/connect` in your Discord server.");
        else if(protocol == ConnJson.ConnProtocol.WEBSOCKET) {
            clientManager.start(connected -> {
                if(!connected) return;
                clientManager.chat("", ConnJson.ChatChannel.ChatChannelType.START, null);
                clientManager.updateStatsChannel(ConnJson.StatsChannel.StatChannelEvent.ONLINE);
            });
        }
        else {
            logger.warn(MinecraftChatColor.GOLD + "**Your server is using the deprecated backup connection method and will be disconnected. Please reconnect in Discord using `/connect`.**");
            clientManager.disconnectForce();
        }
    }

    private @Nullable ConnJson loadConn() {
        Path connJsonPath = Paths.get(server.getDataFolder() + "/connection.conn");
        if(Files.exists(connJsonPath)) {
            try(Reader connReader = Files.newBufferedReader(Paths.get(server.getDataFolder() + "/connection.conn"))) {
                connJson = GSON.fromJson(connReader, ConnJson.class);
                return connJson;
            }
            catch(IOException ignored) {
                logger.error("Failed to read connection.conn file! Please restart your server or reconnect in Discord using `/connect`.");
            }
            catch(JsonSyntaxException | JsonIOException e) {
                logger.error("Your connection data is corrupted! Please reconnect in Discord using `/connect`.");
            }
        }
        return null;
    }
}