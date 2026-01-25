package me.lianecx.discordlinker.spigot.implementation;

import me.lianecx.discordlinker.common.abstraction.*;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static me.lianecx.discordlinker.spigot.util.URLComponent.buildURLComponent;

public class SpigotServer implements LinkerServer {

    private final String dataFolder;

    public SpigotServer(String dataFolder) {
        this.dataFolder = dataFolder;
    }

    @Override
    public String getServerName() {
        return Bukkit.getServer().getName();
    }

    @Override
    public int getMaxPlayers() {
        return Bukkit.getMaxPlayers();
    }

    @Override
    public int getOnlinePlayersCount() {
        return Bukkit.getOnlinePlayers().size();
    }

    @Override
    public List<LinkerPlayer> getOnlinePlayers() {
        return Bukkit.getOnlinePlayers()
                .stream()
                .map(SpigotPlayer::new)
                .collect(Collectors.toList());
    }

    @Override
    public String getDataFolder() {
        return dataFolder;
    }

    @Override
    public @Nullable LinkerPlayer getPlayer(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        return player != null ? new SpigotPlayer(player) : null;
    }

    @Override
    public @Nullable LinkerOfflinePlayer getOfflinePlayer(String username) {
        try {
            // Try to get online player first
            Player onlinePlayer = Bukkit.getPlayerExact(username);
            if (onlinePlayer != null) return new SpigotPlayer(onlinePlayer);

            // If online player exists, return that
            OfflinePlayer player = Bukkit.getOfflinePlayer(username);
            return new LinkerOfflinePlayer(player.getUniqueId().toString(), player.getName());
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public @Nullable LinkerOfflinePlayer getOfflinePlayer(UUID uuid) {
        try {
            // Try to get online player first
            Player onlinePlayer = Bukkit.getPlayer(uuid);
            if (onlinePlayer != null) return new SpigotPlayer(onlinePlayer);

            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            return new LinkerOfflinePlayer(player.getUniqueId().toString(), player.getName());
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void broadcastMessage(String message) {
        Bukkit.broadcastMessage(message);
    }

    @Override
    public void broadcastMessageWithClickableURLs(String message) {
        BaseComponent[] component = buildURLComponent(message);
        Bukkit.getServer().spigot().broadcast(component);
    }

    @Override
    public boolean isPluginOrModEnabled(String pluginOrModName) {
        return Bukkit.getPluginManager().isPluginEnabled(pluginOrModName);
    }

    @Override
    public boolean isOnline() {
        return Bukkit.getServer().getOnlineMode();
    }

    @Override
    public String getMinecraftVersion() {
        return Bukkit.getServer().getBukkitVersion().split("-")[0];
    }

    @Override
    public String getWorldPath() {
        return Bukkit.getWorlds().get(0).getWorldFolder().getAbsolutePath();
    }

    @Override
    public String getWorldContainerPath() {
        return Bukkit.getWorldContainer().getAbsolutePath();
    }

    @Override
    public @Nullable String getFloodgatePrefix() {
        //Load yaml file
        File floodgateConfig = new File("plugins/floodgate/config.yml");
        if(!floodgateConfig.exists()) return null;

        Configuration config = YamlConfiguration.loadConfiguration(floodgateConfig);
        return config.getString("username-prefix");
    }

    @Override
    public String runCommand(String command) {
        //TODO implement
        return "";
    }
}
