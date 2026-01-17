package me.lianecx.discordlinker.common;

import com.google.gson.*;
import me.lianecx.discordlinker.common.abstraction.LinkerServer;
import me.lianecx.discordlinker.common.util.JsonUtil;
import me.lianecx.discordlinker.common.util.MinecraftChatColor;
import me.lianecx.discordlinker.common.abstraction.core.LinkerConfig;
import me.lianecx.discordlinker.common.abstraction.core.LinkerLogger;
import me.lianecx.discordlinker.common.abstraction.core.LinkerScheduler;
import me.lianecx.discordlinker.common.network.client.ClientManager;
import org.jetbrains.annotations.Nullable;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static me.lianecx.discordlinker.common.ConnJson.CONNJSON_FILENAME;

public class DiscordLinkerCommon {

    private static DiscordLinkerCommon discordLinker;

    private final ClientManager clientManager;

    private final LinkerLogger logger;
    private final LinkerConfig config;
    private final LinkerServer server;
    private final LinkerScheduler scheduler;

    private ConnJson connJson;

    private DiscordLinkerCommon(LinkerLogger logger, LinkerConfig config, LinkerServer server, LinkerScheduler scheduler) {
        this.logger = logger;
        this.config = config;
        this.server = server;
        this.scheduler = scheduler;

        this.connJson = loadConn();

        String token = connJson != null ? connJson.getToken() : null;
        this.clientManager = token != null ? new ClientManager(token) : new ClientManager();

        ClientManager.checkVersion();
        reconnectToBot();

        logger.info(MinecraftChatColor.GREEN + "Discord-Linker enabled.");
    }

    public static synchronized DiscordLinkerCommon init(LinkerLogger logger, LinkerConfig config, LinkerServer server, LinkerScheduler scheduler) {
        if (discordLinker != null) throw new IllegalStateException("DiscordLinkerCommon already initialized!");
        discordLinker = new DiscordLinkerCommon(logger, config, server, scheduler);
        return discordLinker;
    }

    public static DiscordLinkerCommon getInstance() {
        if(discordLinker == null) throw new IllegalStateException("DiscordLinkerCommon has not been initialized yet!");
        return discordLinker;
    }

    public static void shutdown() {
        getClientManager().chat(ConnJson.ChatChannel.ChatChannelType.CLOSE);
        getClientManager().updateStatsChannel(ConnJson.StatsChannel.StatChannelEvent.OFFLINE);
        getClientManager().updateStatsChannel(ConnJson.StatsChannel.StatChannelEvent.MEMBERS);
        getClientManager().disconnect();

        getLogger().info(MinecraftChatColor.RED + "Discord-Linker disabled.");
        discordLinker = null;
    }

    public static boolean deleteConn() {
        discordLinker.connJson = null;

        Path connJsonPath = Paths.get(getServer().getDataFolder() + CONNJSON_FILENAME);
        if(!Files.exists(connJsonPath)) return true;

        try {
            Files.delete(connJsonPath);
            return true;
        }
        catch(IOException e) {
            getLogger().error(MinecraftChatColor.RED + "Failed to delete connection data file!");
            e.printStackTrace();
            return false;
        }
    }

    public static boolean writeConn() {
        try (FileWriter writer = new FileWriter(getServer().getDataFolder() + CONNJSON_FILENAME)) {
            writer.write(JsonUtil.toJsonString(getConnJson()));
            return true;
        }
        catch(IOException e) {
            getLogger().error(MinecraftChatColor.RED + "Failed to write connection data to file!");
            e.printStackTrace();
            return false;
        }
    }

    public static boolean updateConn(JsonObject connJson) {
        getInstance().connJson = JsonUtil.getConnJson(connJson);
        return writeConn();
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

    private void reconnectToBot() {
        ConnJson.ConnProtocol protocol = connJson != null ? connJson.getProtocol() : null;
        if(protocol == null)
            logger.warn(MinecraftChatColor.YELLOW + "No Discord server connected! Please invite the \"MC-Linker\" Discord-Bot (https://discord.com/discovery/applications/712759741528408064) and run `/connect` in your Discord server.");
        else if(protocol == ConnJson.ConnProtocol.WEBSOCKET) {
            clientManager.reconnect(connected -> {
                if(!connected) return;
                clientManager.chat(ConnJson.ChatChannel.ChatChannelType.START);
                clientManager.updateStatsChannel(ConnJson.StatsChannel.StatChannelEvent.ONLINE);
            });
        }
        else {
            logger.warn(MinecraftChatColor.GOLD + "**Your server is using the deprecated backup connection method and will be disconnected. Please reconnect in Discord using `/connect`.**");
            clientManager.disconnectForce();
        }
    }

    private @Nullable ConnJson loadConn() {
        Path connJsonPath = Paths.get(server.getDataFolder() + CONNJSON_FILENAME);
        if(Files.exists(connJsonPath)) {
            try(Reader connReader = Files.newBufferedReader(Paths.get(server.getDataFolder() + CONNJSON_FILENAME))) {
                connJson = JsonUtil.getConnJson(connReader);
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