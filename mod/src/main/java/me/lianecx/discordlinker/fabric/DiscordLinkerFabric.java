package me.lianecx.discordlinker.fabric;

import me.lianecx.discordlinker.architectury.DiscordLinkerMod;
import net.fabricmc.api.DedicatedServerModInitializer;

public class DiscordLinkerFabric implements DedicatedServerModInitializer {

    @Override
    public void onInitializeServer() {
        DiscordLinkerMod.init();
    }
}