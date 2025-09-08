//? if fabric {
package me.lianecx.discordlinker.fabric;

import me.lianecx.discordlinker.common.DiscordLinkerCommon;
import net.fabricmc.api.ModInitializer;

public class DiscordLinkerFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        DiscordLinkerCommon.init();
    }
}
//?}