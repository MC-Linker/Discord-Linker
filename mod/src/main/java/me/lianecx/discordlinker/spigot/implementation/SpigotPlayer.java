package me.lianecx.discordlinker.spigot.implementation;

import me.lianecx.discordlinker.common.abstraction.LinkerPlayer;
import org.bukkit.entity.Player;

public class SpigotPlayer extends LinkerPlayer {

    private final Player player;

    public SpigotPlayer(Player player) {
        super(player.getUniqueId().toString(), player.getName());
        this.player = player;
    }

    @Override
    public void sendMessage(String message) {
        player.sendMessage(message);
    }

    @Override
    public boolean hasPermission(String permission) {
        return player.hasPermission(permission);
    }
}
