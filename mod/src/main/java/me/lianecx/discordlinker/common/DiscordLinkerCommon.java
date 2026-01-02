package me.lianecx.discordlinker.common;

import me.lianecx.discordlinker.common.abstraction.event.LinkerEventBus;
import me.lianecx.discordlinker.common.abstraction.event.PlayerJoinEvent;

/**
 * This is the entry point of the common side, called by each loader's specific side.
 */
public class DiscordLinkerCommon {

    private static DiscordLinkerCommon instance = null;

    public static DiscordLinkerCommon getInstance() {
        if(instance == null) instance = new DiscordLinkerCommon();
        return instance;
    }

    public static void init() {
        System.out.println("Hello from DiscordLinkerCommon!");
    }
}
