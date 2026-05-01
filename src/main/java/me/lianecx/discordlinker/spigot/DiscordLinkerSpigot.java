package me.lianecx.discordlinker.spigot;

import me.lianecx.discordlinker.common.DiscordLinkerCommon;
import me.lianecx.discordlinker.common.hooks.HookProvider;
import me.lianecx.discordlinker.common.hooks.luckperms.LuckPermsHookProvider;
import me.lianecx.discordlinker.common.util.MinecraftVersionUtil;
import me.lianecx.discordlinker.spigot.hooks.vault.VaultHookProvider;
import me.lianecx.discordlinker.spigot.implementation.*;
import org.bukkit.plugin.java.JavaPlugin;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getInstance;

public class DiscordLinkerSpigot extends JavaPlugin {

    @Override
    public void onEnable() {
        SpigotConfig config = new SpigotConfig(this);
        SpigotServer server = new SpigotServer(getDataFolder().getAbsolutePath());
        DiscordLinkerCommon.init(
                new SpigotLogger(this.getLogger(), config.isTestVersion()),
                config,
                server,
                new SpigotScheduler(this),
                new SpigotTeamsBridge(),
                new HookProvider[]{ new LuckPermsHookProvider(), new VaultHookProvider() }
        );

        this.getServer().getPluginManager().registerEvents(new SpigotEvents(), this);

        if (MinecraftVersionUtil.isAtLeast(server.getMinecraftVersion(), "1.12"))
            this.getServer().getPluginManager().registerEvents(new SpigotAdvancementListener(), this);
        else this.getServer().getPluginManager().registerEvents(new SpigotAchievementListener(), this);

        getCommand("linker").setExecutor(new SpigotCommands());
        getCommand("linker").setTabCompleter(new SpigotCommands());
        getCommand("discord").setExecutor(new SpigotCommands());
        getCommand("verify").setExecutor(new SpigotCommands());
        getCommand("dm").setExecutor(new SpigotCommands());
    }

    @Override
    public void onDisable() {
        getInstance().shutdown();
    }
}
