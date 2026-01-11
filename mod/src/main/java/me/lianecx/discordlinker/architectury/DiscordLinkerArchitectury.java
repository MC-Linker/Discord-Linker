package me.lianecx.discordlinker.architectury;

import me.lianecx.discordlinker.architectury.implementation.ArchitecturyConfig;
import me.lianecx.discordlinker.architectury.implementation.ArchitecturyLogger;
import me.lianecx.discordlinker.architectury.implementation.ArchitecturyServer;
import me.lianecx.discordlinker.common.DiscordLinkerCommon;

import static dev.architectury.utils.GameInstance.getServer;

public class DiscordLinkerArchitectury {

    public static final String MOD_ID = "discordlinker";

    private static DiscordLinkerArchitectury instance = null;
    private static DiscordLinkerCommon common = null;

    private DiscordLinkerArchitectury() {}

    public static synchronized DiscordLinkerArchitectury init() {
        if(common != null) throw new IllegalStateException("DiscordLinkerArchitectury is already initialized!");
        ArchitecturyLogger logger = new ArchitecturyLogger();
        ArchitecturyConfig config = new ArchitecturyConfig(".");
        ArchitecturyServer server = new ArchitecturyServer(getServer());

        common = DiscordLinkerCommon.init(logger, config, server);
        instance = new DiscordLinkerArchitectury();
        return instance;
    }

    public static DiscordLinkerArchitectury getInstance() {
        if(instance == null) throw new IllegalStateException("DiscordLinkerArchitectury has not been initialized yet!");
        return instance;
    }
}