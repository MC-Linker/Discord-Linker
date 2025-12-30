package me.lianecx.discordlinker.spigot;

import me.lianecx.discordlinker.common.DiscordLinkerCommon;
import org.bukkit.plugin.java.JavaPlugin;

public class DiscordLinkerSpigot extends JavaPlugin {

     @Override
     public void onEnable() {
        DiscordLinkerCommon.init();
    }
}