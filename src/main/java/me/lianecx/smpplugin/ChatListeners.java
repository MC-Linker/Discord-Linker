package me.lianecx.smpplugin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.server.ServerCommandEvent;

public class ChatListeners implements Listener  {

    private static final SMPPlugin PLUGIN = SMPPlugin.getPlugin();

    @EventHandler
    public void onChatmessage(AsyncPlayerChatEvent event) {
        PLUGIN.getServer().getScheduler().runTaskAsynchronously(PLUGIN, () ->
            HttpConnection.send(event.getMessage(), 0, event.getPlayer().getName()));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        PLUGIN.getServer().getScheduler().runTaskAsynchronously(PLUGIN, () ->
            HttpConnection.send(event.getJoinMessage(), 1, event.getPlayer().getName()));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        PLUGIN.getServer().getScheduler().runTaskAsynchronously(PLUGIN, () ->
            HttpConnection.send(event.getQuitMessage(), 2, event.getPlayer().getName()));
    }

    @EventHandler
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        PLUGIN.getServer().getScheduler().runTaskAsynchronously(PLUGIN, () ->
            HttpConnection.send(event.getAdvancement().getKey().toString(), 3, event.getPlayer().getName()));
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        PLUGIN.getServer().getScheduler().runTaskAsynchronously(PLUGIN, () ->
            HttpConnection.send(event.getDeathMessage(), 4, event.getEntity().getName()));
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        PLUGIN.getServer().getScheduler().runTaskAsynchronously(PLUGIN, () ->
            HttpConnection.send(event.getMessage(), 5, event.getPlayer().getName()));
    }

    @EventHandler
    public void onServerCommand(ServerCommandEvent event) {
        PLUGIN.getServer().getScheduler().runTaskAsynchronously(PLUGIN, () ->
            HttpConnection.send(event.getCommand(), 5, event.getSender().getName()));
    }
}
