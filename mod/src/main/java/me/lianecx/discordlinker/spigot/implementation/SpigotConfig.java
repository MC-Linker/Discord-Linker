package me.lianecx.discordlinker.spigot.implementation;

import me.lianecx.discordlinker.common.abstraction.core.LinkerConfig;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class SpigotConfig implements LinkerConfig {

    private final JavaPlugin plugin;

    private final FileConfiguration config;

    public SpigotConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    @Override
    public String getPluginVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public int getBotPort() {
        return config.getInt("bot_port");
    }

    @Override
    public void setBotPort(int port) {
        config.set("bot_port", port);
        plugin.saveConfig();
    }

    @Override
    public String getTemplateChatMessage() {
        return config.getString("message");
    }

    @Override
    public String getTemplatePrivateMessage() {
        return config.getString("private_mesage");
    }

    @Override
    public String getTemplateReplyMessage() {
        return config.getString("reply_message");
    }

    @Override
    public boolean shouldDebug() {
        return config.getBoolean("debug");
    }
}
