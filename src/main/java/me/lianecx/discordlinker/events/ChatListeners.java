package me.lianecx.discordlinker.events;

import me.lianecx.discordlinker.DiscordLinker;
import me.lianecx.discordlinker.network.ChatType;
import me.lianecx.discordlinker.network.StatsUpdateEvent;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.server.ServerCommandEvent;

public class ChatListeners implements Listener {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onChatMessage(AsyncPlayerChatEvent event) {
        //Remove color codes
        String replacedMessage = ChatColor.stripColor(event.getMessage().replaceAll("(?i)&[0-9A-FK-OR]", ""));
        sendChatAsync(replacedMessage, ChatType.CHAT, event.getPlayer().getName());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        sendChatAsync(event.getJoinMessage(), ChatType.JOIN, event.getPlayer().getName());
        sendStatsAsync(StatsUpdateEvent.MEMBERS);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        sendChatAsync(event.getQuitMessage(), ChatType.QUIT, event.getPlayer().getName());
        sendStatsAsync(StatsUpdateEvent.MEMBERS);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        //Dont process recipes
        if(event.getAdvancement().getKey().toString().startsWith("minecraft:recipes/")) return;
        sendChatAsync(event.getAdvancement().getKey().toString(), ChatType.ADVANCEMENT, event.getPlayer().getName());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        sendChatAsync(event.getDeathMessage(), ChatType.DEATH, event.getEntity().getName());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        sendChatAsync(event.getMessage(), ChatType.PLAYER_COMMAND, event.getPlayer().getName());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onConsoleCommand(ServerCommandEvent event) {
        //Command sender either from the console or a command block
        ChatType commandType = event.getSender() instanceof ConsoleCommandSender ? ChatType.CONSOLE_COMMAND : ChatType.BLOCK_COMMAND;
        sendChatAsync(event.getCommand(), commandType, event.getSender().getName());
    }

    public void sendChatAsync(String message, ChatType type, String sender) {
        DiscordLinker.getPlugin().getServer().getScheduler().runTaskAsynchronously(DiscordLinker.getPlugin(), () ->
                DiscordLinker.getAdapterManager().sendChat(message, type, sender));
    }

    public void sendStatsAsync(StatsUpdateEvent type) {
        DiscordLinker.getPlugin().getServer().getScheduler().runTaskAsynchronously(DiscordLinker.getPlugin(), () ->
                DiscordLinker.getAdapterManager().sendStatsUpdate(type));
    }
}
