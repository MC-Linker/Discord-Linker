package me.lianecx.discordlinker.forge;

import me.lianecx.discordlinker.architectury.DiscordLinkerMod;
import net.minecraftforge.fml.common.Mod;

/**
 * This is the entry point for the forge side.
 */
@Mod("discordlinker")
public class DiscordLinkerForge {
    public DiscordLinkerForge() {
        //? if >1.16.5
        ForgePreLoginEvent.register();
        DiscordLinkerMod.init();
    }
}
