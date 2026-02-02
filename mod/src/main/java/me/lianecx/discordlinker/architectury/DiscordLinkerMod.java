package me.lianecx.discordlinker.architectury;

import dev.architectury.event.events.common.LifecycleEvent;
import me.lianecx.discordlinker.architectury.implementation.*;
import me.lianecx.discordlinker.common.DiscordLinkerCommon;

public class DiscordLinkerMod {

    public static final String MOD_ID = "discordlinker";

    private static DiscordLinkerCommon common = null;

    private DiscordLinkerMod() {}

    public static synchronized void init() {
        if(common != null) throw new IllegalStateException("DiscordLinkerArchitectury is already initialized!");

        ModCommands.registerCommands();
        ModEvents.registerEvents();
        LifecycleEvent.SERVER_STARTED.register(instance -> {
            ModServer server = new ModServer(instance);
            ModConfig config = new ModConfig(server.getDataFolder());

            common = DiscordLinkerCommon.init(new ModLogger(), config, server, new ModScheduler(), new ModTeamsBridge(server));
        });

        LifecycleEvent.SERVER_STOPPING.register(instance -> {
            common.shutdown();
        });
    }
}