package me.lianecx.smpplugin;

import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.server.ServerCommandEvent;

public class ChatListeners implements Listener  {

    private static final SMPPlugin PLUGIN = SMPPlugin.getPlugin();

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onChatMessage(AsyncPlayerChatEvent event) {
        PLUGIN.getServer().getScheduler().runTaskAsynchronously(PLUGIN, () ->
            HttpConnection.send(event.getMessage(), "chat", event.getPlayer().getName()));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        PLUGIN.getServer().getScheduler().runTaskAsynchronously(PLUGIN, () ->
            HttpConnection.send(event.getJoinMessage(), "join", event.getPlayer().getName()));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {
        PLUGIN.getServer().getScheduler().runTaskAsynchronously(PLUGIN, () ->
            HttpConnection.send(event.getQuitMessage(), "quit", event.getPlayer().getName()));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        PLUGIN.getServer().getScheduler().runTaskAsynchronously(PLUGIN, () ->
            HttpConnection.send(event.getAdvancement().getKey().toString(), "advancement", event.getPlayer().getName()));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        PLUGIN.getServer().getScheduler().runTaskAsynchronously(PLUGIN, () ->
            HttpConnection.send(event.getDeathMessage(), "death", event.getEntity().getName()));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        PLUGIN.getServer().getScheduler().runTaskAsynchronously(PLUGIN, () ->
            HttpConnection.send(event.getMessage(), "player_command", event.getPlayer().getName()));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onConsoleCommand(ServerCommandEvent event) {
        PLUGIN.getServer().getScheduler().runTaskAsynchronously(PLUGIN, () -> {
            //Command sender either from the console or a command block
            String commandType = event.getSender() instanceof ConsoleCommandSender ? "console_command" : "block_command";
            HttpConnection.send(event.getCommand(), commandType, event.getSender().getName());
        });
    }
}
