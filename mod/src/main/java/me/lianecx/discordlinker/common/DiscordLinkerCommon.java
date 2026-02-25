package me.lianecx.discordlinker.common;

import me.lianecx.discordlinker.common.abstraction.LinkerServer;
import me.lianecx.discordlinker.common.abstraction.TeamsBridge;
import me.lianecx.discordlinker.common.abstraction.core.LinkerConfig;
import me.lianecx.discordlinker.common.abstraction.core.LinkerLogger;
import me.lianecx.discordlinker.common.abstraction.core.LinkerScheduler;
import me.lianecx.discordlinker.common.commands.LinkerMinecraftCommandBus;
import me.lianecx.discordlinker.common.events.LinkerMinecraftEventBus;
import me.lianecx.discordlinker.common.events.data.ServerStartEventData;
import me.lianecx.discordlinker.common.events.data.ServerStopEventData;
import me.lianecx.discordlinker.common.network.client.ClientManager;
import me.lianecx.discordlinker.common.network.protocol.events.LinkerDiscordEventBus;
import me.lianecx.discordlinker.common.util.MinecraftChatColor;
import org.jetbrains.annotations.Nullable;

public class DiscordLinkerCommon {

    private static DiscordLinkerCommon discordLinker;

    private final ClientManager clientManager;

    private final LinkerLogger logger;
    private final LinkerConfig config;
    private final LinkerServer server;
    private final LinkerScheduler scheduler;
    private final TeamsAndGroupsBridge teamsAndGroupsBridge;

    private final LinkerDiscordEventBus discordEventBus;
    private final LinkerMinecraftEventBus minecraftEventBus;
    private final LinkerMinecraftCommandBus minecraftCommandBus;

    private ConnJson connJson;

    private DiscordLinkerCommon(LinkerLogger logger, LinkerConfig config, LinkerServer server, LinkerScheduler scheduler, TeamsAndGroupsBridge teamsAndGroupsBridge) {
        this.logger = logger;
        this.config = config;
        this.server = server;
        this.scheduler = scheduler;
        this.teamsAndGroupsBridge = teamsAndGroupsBridge;

        this.discordEventBus = new LinkerDiscordEventBus();
        this.minecraftEventBus = new LinkerMinecraftEventBus();
        this.minecraftCommandBus = new LinkerMinecraftCommandBus();

        this.connJson = ConnJson.load(server, logger);

        String token = connJson != null ? connJson.getToken() : null;
        //If snapshot version, request test-bot at port 81 otherwise request main-bot at port 80/config-port
        int botPort = config.isTestVersion() ? 81 : config.getBotPort();
        this.clientManager = token != null ? new ClientManager(token, botPort, server, discordEventBus) : new ClientManager(discordEventBus, botPort);
    }

    public static synchronized DiscordLinkerCommon init(LinkerLogger logger, LinkerConfig config, LinkerServer server, LinkerScheduler scheduler, TeamsBridge teamsBridge) {
        if(discordLinker != null) throw new IllegalStateException("DiscordLinkerCommon already initialized!");
        // Initialize instance with fields
        discordLinker = new DiscordLinkerCommon(logger, config, server, scheduler, new TeamsAndGroupsBridge(server, teamsBridge));
        logger.debug(discordLinker.teamsAndGroupsBridge.isLuckPermsEnabled() ? "Detected LuckPerms." : "LuckPerms not detected.");

        // Init logic
        ClientManager.checkVersion();
        discordLinker.reconnectToBot();

        logger.info(MinecraftChatColor.GREEN + "Discord-Linker enabled.");
        return discordLinker;
    }

    public static DiscordLinkerCommon getInstance() {
        if(discordLinker == null) throw new IllegalStateException("DiscordLinkerCommon has not been initialized yet!");
        return discordLinker;
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

    public static void setConnJson(@Nullable ConnJson connJson) {
        getInstance().connJson = connJson;
    }

    public static ClientManager getClientManager() {
        return getInstance().clientManager;
    }

    public static LinkerServer getServer() {
        return getInstance().server;
    }

    public static LinkerScheduler getScheduler() {
        return getInstance().scheduler;
    }

    public static TeamsAndGroupsBridge getTeamsAndGroupsBridge() {
        return getInstance().teamsAndGroupsBridge;
    }

    public static LinkerDiscordEventBus getDiscordEventBus() {
        return getInstance().discordEventBus;
    }

    public static LinkerMinecraftEventBus getMinecraftEventBus() {
        return getInstance().minecraftEventBus;
    }

    public static LinkerMinecraftCommandBus getMinecraftCommandBus() {
        return getInstance().minecraftCommandBus;
    }

    public void shutdown() {
        minecraftEventBus.emit(new ServerStopEventData());
        clientManager.disconnect();
        scheduler.shutdown();

        if(getConnJson() != null) getConnJson().write();

        logger.info(MinecraftChatColor.RED + "Discord-Linker disabled.");
        discordLinker = null;
    }

    private void reconnectToBot() {
        ConnJson.ConnProtocol protocol = connJson != null ? connJson.getProtocol() : null;
        if(protocol == null)
            logger.warn(MinecraftChatColor.YELLOW + "No Discord server connected! Please invite the \"MC-Linker\" Discord-Bot (https://discord.com/discovery/applications/712759741528408064) and run `/connect` in your Discord server.");
        else if(protocol == ConnJson.ConnProtocol.WEBSOCKET) {
            clientManager.reconnect().thenAccept(connected -> {
                if(!connected) return;
                minecraftEventBus.emit(new ServerStartEventData());
            });
        }
        else {
            logger.warn(MinecraftChatColor.GOLD + "**Your server is using the deprecated backup connection method and will be disconnected. Please reconnect in Discord using `/connect`.**");
            clientManager.disconnectForce();
        }
    }
}