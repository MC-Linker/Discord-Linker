//? if fabric {
package me.lianecx.discordlinker.fabric;

import me.lianecx.discordlinker.common.DiscordLinkerCommon;
import net.fabricmc.api.DedicatedServerModInitializer;

public class DiscordLinkerFabric implements DedicatedServerModInitializer {

    @Override
    public void onInitializeServer() {
        DiscordLinkerCommon.init();
    }
}
//?}