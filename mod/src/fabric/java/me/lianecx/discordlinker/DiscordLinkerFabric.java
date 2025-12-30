package me.lianecx.discordlinker;

import net.fabricmc.api.DedicatedServerModInitializer;

public class DiscordLinkerFabric implements DedicatedServerModInitializer {

    @Override
    public void onInitializeServer() {
        DiscordLinkerArchitectury.init();
    }
}