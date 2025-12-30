package me.lianecx.discordlinker.fabric;

import me.lianecx.discordlinker.architectury.DiscordLinkerArchitectury;
import net.fabricmc.api.DedicatedServerModInitializer;

public class DiscordLinkerFabric implements DedicatedServerModInitializer {

    @Override
    public void onInitializeServer() {
        DiscordLinkerArchitectury.init();
    }
}