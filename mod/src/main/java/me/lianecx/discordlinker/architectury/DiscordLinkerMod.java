package me.lianecx.discordlinker.architectury;

//? if <=1.16.5 {
/*import dev.architectury.event.events.LifecycleEvent;
*///? } else
import dev.architectury.event.events.common.LifecycleEvent;
import me.lianecx.discordlinker.architectury.implementation.*;
import me.lianecx.discordlinker.common.DiscordLinkerCommon;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getInstance;

public class DiscordLinkerMod {

    public static final String MOD_ID = "discordlinker";

    private DiscordLinkerMod() {}

    public static synchronized void init() {
        ModCommands.registerCommands();
        ModEvents.registerEvents();
        LifecycleEvent.SERVER_STARTED.register(instance -> {
            ModServer server = new ModServer(instance);
            ModConfig config = new ModConfig(server.getDataFolder());

            DiscordLinkerCommon.init(new ModLogger(config.isTestVersion()), config, server, new ModScheduler(instance), new ModTeamsBridge());
        });

        LifecycleEvent.SERVER_STOPPING.register(instance -> getInstance().shutdown());
    }
}