package me.lianecx.discordlinker.spigot.implementation;

import me.lianecx.discordlinker.common.abstraction.*;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
            OfflinePlayer player = Bukkit.getOfflinePlayer(username);
            return new SpigotOfflinePlayer(player);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public @Nullable LinkerOfflinePlayer getOfflinePlayer(UUID uuid) {
        try {
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            return new SpigotOfflinePlayer(player);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void broadcastMessage(String message) {
        Bukkit.broadcastMessage(message);
    }
}
