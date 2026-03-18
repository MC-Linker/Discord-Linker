package me.lianecx.discordlinker.spigot.implementation;

import me.lianecx.discordlinker.common.abstraction.LinkerPlayer;
import me.lianecx.discordlinker.spigot.util.NBTParser;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.entity.Player;

import static me.lianecx.discordlinker.spigot.util.URLComponent.buildURLComponent;

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
    public void sendMessageWithClickableURLs(String message) {
        BaseComponent[] component = buildURLComponent(message);
        player.spigot().sendMessage(component);
    }

    @Override
    public boolean hasPermission(int defaultLevel, String permission) {
        return player.hasPermission(permission);
    }

    @Override
    public void kick(String reason) {
        player.kickPlayer(reason);
    }

    @Override
    public String getNBTAsString() {
        return NBTParser.parsePlayerNBT(player);
    }
}
