package me.lianecx.discordlinker.architectury;

//? if <=1.16.5 {
/*import dev.architectury.event.events.LifecycleEvent;
*///? } else
import dev.architectury.event.events.common.LifecycleEvent;
import me.lianecx.discordlinker.architectury.hybrid.BukkitEventBridge;
import me.lianecx.discordlinker.architectury.implementation.*;
import me.lianecx.discordlinker.common.DiscordLinkerCommon;
import me.lianecx.discordlinker.common.hooks.HookProvider;
import me.lianecx.discordlinker.common.hooks.luckperms.LuckPermsHookProvider;

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

            DiscordLinkerCommon.init(new ModLogger(config.isTestVersion()), config, server, new ModScheduler(instance), new ModTeamsBridge(), new HookProvider[]{ new LuckPermsHookProvider() });

            if (BukkitEventBridge.isHybridServer()) BukkitEventBridge.register();
        });

        LifecycleEvent.SERVER_STOPPING.register(instance -> getInstance().shutdown());
    }
}