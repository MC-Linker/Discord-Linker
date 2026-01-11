package me.lianecx.discordlinker.neoforge;

import me.lianecx.discordlinker.architectury.DiscordLinkerArchitectury;
import net.minecraftforge.fml.common.Mod;

/**
 * This is the entry point for the neoforge side.
 */
@Mod("discordlinker")
public class DiscordLinkerNeoforge {
    public DiscordLinkerNeoforge() {
        DiscordLinkerArchitectury.init();
    }
}