package me.lianecx.discordlinker;

import net.minecraftforge.fml.common.Mod;
import me.lianecx.discordlinker.DiscordLinkerArchitectury;

/**
 * This is the entry point for the forge side.
 */
@Mod("discordlinker")
public class DiscordLinkerForge {
    public DiscordLinkerForge() {
        DiscordLinkerArchitectury.init();
    }
}