package me.lianecx.discordlinker.forge;

import me.lianecx.discordlinker.architectury.DiscordLinkerArchitectury;
import net.minecraftforge.fml.common.Mod;

/**
 * This is the entry point for the forge side.
 */
@Mod("discordlinker")
public class DiscordLinkerForge {
    public DiscordLinkerForge() {
        DiscordLinkerArchitectury.init();
    }
}