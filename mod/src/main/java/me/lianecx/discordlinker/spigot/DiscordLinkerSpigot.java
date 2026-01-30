package me.lianecx.discordlinker.spigot;

import me.lianecx.discordlinker.common.DiscordLinkerCommon;
import me.lianecx.discordlinker.spigot.implementation.SpigotConfig;
import me.lianecx.discordlinker.spigot.implementation.SpigotLogger;
import me.lianecx.discordlinker.spigot.implementation.SpigotScheduler;
import me.lianecx.discordlinker.spigot.implementation.SpigotServer;
import org.bukkit.plugin.java.JavaPlugin;

public class DiscordLinkerSpigot extends JavaPlugin {

    @Override
    public void onEnable() {
        DiscordLinkerCommon.init(new SpigotLogger(getLogger()), new SpigotConfig(this), new SpigotServer(getDataFolder().getAbsolutePath()), new SpigotScheduler(this), new SpigotTeamsAndGroupsBridge());
    }
}