package me.lianecx.discordlinker;

import me.lianecx.discordlinker.DiscordLinkerArchitectury;
import net.neoforged.fml.common.Mod;

/**
 * This is the entry point for the neoforge side.
 */
@Mod("discordlinker")
public class DiscordLinkerNeoforge {
    public DiscordLinkerNeoforge() {
        DiscordLinkerArchitectury.init();
    }
}