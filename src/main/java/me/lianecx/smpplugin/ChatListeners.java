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
            HttpConnection.send(event.getMessage(), "chat", event.getPlayer().getName()));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        PLUGIN.getServer().getScheduler().runTaskAsynchronously(PLUGIN, () ->
            HttpConnection.send(event.getJoinMessage(), "join", event.getPlayer().getName()));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        PLUGIN.getServer().getScheduler().runTaskAsynchronously(PLUGIN, () ->
            HttpConnection.send(event.getQuitMessage(), "quit", event.getPlayer().getName()));
    }

    @EventHandler
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        PLUGIN.getServer().getScheduler().runTaskAsynchronously(PLUGIN, () ->
            HttpConnection.send(event.getAdvancement().getKey().toString(), "advancement", event.getPlayer().getName()));
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        PLUGIN.getServer().getScheduler().runTaskAsynchronously(PLUGIN, () ->
            HttpConnection.send(event.getDeathMessage(), "death", event.getEntity().getName()));
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        PLUGIN.getServer().getScheduler().runTaskAsynchronously(PLUGIN, () ->
            HttpConnection.send(event.getMessage(), "command", event.getPlayer().getName()));
    }

    @EventHandler
    public void onServerCommand(ServerCommandEvent event) {
        PLUGIN.getServer().getScheduler().runTaskAsynchronously(PLUGIN, () ->
            HttpConnection.send(event.getCommand(), "command", event.getSender().getName()));
    }
}
