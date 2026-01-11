package me.lianecx.discordlinker.spigot.implementation;

import me.lianecx.discordlinker.common.abstraction.LinkerOfflinePlayer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public class SpigotOfflinePlayer extends LinkerOfflinePlayer {

    private final OfflinePlayer player;

    public SpigotOfflinePlayer(OfflinePlayer player) {
        super(player.getUniqueId().toString(), player.getName());
        this.player = player;
    }

    @Override
    public String getUUID() {
        return player.getUniqueId().toString();
    }

    @Override
    public String getName() {
        return player.getName();
    }
}
