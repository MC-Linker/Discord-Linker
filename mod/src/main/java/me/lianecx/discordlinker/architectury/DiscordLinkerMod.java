package me.lianecx.discordlinker.architectury;

import dev.architectury.utils.GameInstance;
import me.lianecx.discordlinker.architectury.implementation.*;
import me.lianecx.discordlinker.common.DiscordLinkerCommon;

public class DiscordLinkerMod {

    public static final String MOD_ID = "discordlinker";

    private static DiscordLinkerMod instance = null;
    private static DiscordLinkerCommon common = null;

    private DiscordLinkerMod() {}

    public static synchronized DiscordLinkerMod init() {
        if(common != null) throw new IllegalStateException("DiscordLinkerArchitectury is already initialized!");
        ModServer server = new ModServer(GameInstance.getServer());
        ModConfig config = new ModConfig(server.getDataFolder());

        common = DiscordLinkerCommon.init(new ModLogger(), config, server, new ModScheduler(), new ModTeamsBridge(server));
        instance = new DiscordLinkerMod();
        return instance;
    }

    public static DiscordLinkerMod getInstance() {
        if(instance == null) throw new IllegalStateException("DiscordLinkerArchitectury has not been initialized yet!");
        return instance;
    }
}