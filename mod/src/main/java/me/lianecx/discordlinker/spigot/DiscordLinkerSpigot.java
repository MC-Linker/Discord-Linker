package me.lianecx.discordlinker.spigot;

import me.lianecx.discordlinker.common.DiscordLinkerCommon;
import me.lianecx.discordlinker.spigot.implementation.*;
import org.bukkit.plugin.java.JavaPlugin;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getInstance;

public class DiscordLinkerSpigot extends JavaPlugin {

    @Override
    public void onEnable() {
        DiscordLinkerCommon.init(
                new SpigotLogger(getLogger()),
                new SpigotConfig(this),
                new SpigotServer(getDataFolder().getAbsolutePath()),
                new SpigotScheduler(this),
                new SpigotTeamsBridge()
        );

        getServer().getPluginManager().registerEvents(new SpigotEvents(), this);
        getCommand("linker").setExecutor(new SpigotCommands());
        getCommand("linker").setTabCompleter(new SpigotCommands());
        getCommand("discord").setExecutor(new SpigotCommands());
        getCommand("verify").setExecutor(new SpigotCommands());
    }

    @Override
    public void onDisable() {
        getInstance().shutdown();
    }
}