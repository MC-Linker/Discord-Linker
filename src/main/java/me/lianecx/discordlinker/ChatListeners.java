package me.lianecx.discordlinker;

import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.server.ServerCommandEvent;

public class ChatListeners implements Listener {

    private static final DiscordLinker PLUGIN = DiscordLinker.getPlugin();

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onChatMessage(AsyncPlayerChatEvent event) {
        //Remove color codes
        String replacedMessage = ChatColor.stripColor(event.getMessage().replaceAll("(?i)&[0-9A-FK-OR]", ""));

        PLUGIN.getServer().getScheduler().runTaskAsynchronously(PLUGIN, () ->
                HttpConnection.sendChat(replacedMessage, ChatType.CHAT, event.getPlayer().getName()));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        PLUGIN.getServer().getScheduler().runTaskAsynchronously(PLUGIN, () ->
                HttpConnection.sendChat(event.getJoinMessage(), ChatType.JOIN, event.getPlayer().getName()));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {
        PLUGIN.getServer().getScheduler().runTaskAsynchronously(PLUGIN, () ->
                HttpConnection.sendChat(event.getQuitMessage(), ChatType.QUIT, event.getPlayer().getName()));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        //Dont process recipes
        if(event.getAdvancement().getKey().toString().startsWith("minecraft:recipes/")) return;

        PLUGIN.getServer().getScheduler().runTaskAsynchronously(PLUGIN, () ->
                HttpConnection.sendChat(event.getAdvancement().getKey().toString(), ChatType.ADVANCEMENT, event.getPlayer().getName()));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        PLUGIN.getServer().getScheduler().runTaskAsynchronously(PLUGIN, () ->
                HttpConnection.sendChat(event.getDeathMessage(), ChatType.DEATH, event.getEntity().getName()));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        PLUGIN.getServer().getScheduler().runTaskAsynchronously(PLUGIN, () ->
                HttpConnection.sendChat(event.getMessage(), ChatType.PLAYER_COMMAND, event.getPlayer().getName()));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onConsoleCommand(ServerCommandEvent event) {
        PLUGIN.getServer().getScheduler().runTaskAsynchronously(PLUGIN, () -> {
            //Command sender either from the console or a command block
            ChatType commandType = event.getSender() instanceof ConsoleCommandSender ? ChatType.CONSOLE_COMMAND : ChatType.BLOCK_COMMAND;
            HttpConnection.sendChat(event.getCommand(), commandType, event.getSender().getName());
        });
    }
}
