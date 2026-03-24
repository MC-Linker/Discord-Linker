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
        plugin.saveDefaultConfig();
    }

    @Override
    public boolean isTestVersion() {
        return getPluginVersion().contains("SNAPSHOT");
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
    public int getSyncCheckIntervalSeconds() {
        return config.getInt("sync_check_interval");
    }

    @Override
    public int getChatConsoleFlushSeconds() {
        return config.getInt("chatconsole_flush_seconds");
    }

    @Override
    public int getChatConsoleMaxChars() {
        return config.getInt("chatconsole_max_chars");
    }

    @Override
    public void reload() {
        plugin.reloadConfig();
    }
}
