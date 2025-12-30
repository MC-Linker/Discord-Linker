package me.lianecx.discordlinker;

import org.bukkit.plugin.java.JavaPlugin;

public class DiscordLinkerSpigot extends JavaPlugin {

     @Override
     public void onEnable() {
        DiscordLinkerCommon.init();
    }
}