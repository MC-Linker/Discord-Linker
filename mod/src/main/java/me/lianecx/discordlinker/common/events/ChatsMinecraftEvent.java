package me.lianecx.discordlinker.common.events;

import me.lianecx.discordlinker.common.ConnJson;
import me.lianecx.discordlinker.common.events.data.*;
import me.lianecx.discordlinker.common.util.MinecraftChatColor;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getClientManager;
import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getScheduler;

public class ChatsMinecraftEvent {

    public static void handleServerStart(ServerStartEventData event) {
        sendStatsAsync(ConnJson.StatsChannel.StatsChannelEvent.ONLINE);
    }

    public static void handleServerStop(ServerStopEventData event) {
        sendStatsAsync(ConnJson.StatsChannel.StatsChannelEvent.OFFLINE);
    }

    public static void handleChat(ChatEventData event) {
        //Remove color codes
        String replacedMessage = MinecraftChatColor.stripColorCodes(event.message);
        sendChatAsync(replacedMessage, ConnJson.ChatChannel.ChatChannelType.CHAT, event.player.getName());
    }

    public static void handleJoin(PlayerJoinEventData event) {
        sendChatAsync(event.joinMessage, ConnJson.ChatChannel.ChatChannelType.JOIN, event.player.getName());
        sendStatsAsync(ConnJson.StatsChannel.StatsChannelEvent.MEMBERS);
    }

    public static void handleQuit(PlayerQuitEventData event) {
        sendChatAsync(event.quitMessage, ConnJson.ChatChannel.ChatChannelType.QUIT, event.player.getName());
        sendStatsAsync(ConnJson.StatsChannel.StatsChannelEvent.MEMBERS);
    }

    public static void handleAdvancement(AdvancementEventData event) {
        //Dont process recipes
        if(event.advancementKey.startsWith("minecraft:recipes/")) return;
        sendChatAsync(event.advancementKey, ConnJson.ChatChannel.ChatChannelType.ADVANCEMENT, event.player.getName());
    }

    public static void handlePlayerDeath(PlayerDeathEventData event) {
        sendChatAsync(event.deathMessage, ConnJson.ChatChannel.ChatChannelType.DEATH, event.player.getName());
    }

    public static void handlePlayerCommand(PlayerCommandEventData event) {
        sendChatAsync(event.command, ConnJson.ChatChannel.ChatChannelType.PLAYER_COMMAND, event.player.getName());
    }

    public static void handleConsoleCommand(ConsoleCommandEventData event) {
        //TODO name of console?
        sendChatAsync(event.command, ConnJson.ChatChannel.ChatChannelType.CONSOLE_COMMAND, event.senderName);
    }

    public static void handleBlockCommand(BlockCommandEventData event) {
        //TODO name of command block?
        sendChatAsync(event.command, ConnJson.ChatChannel.ChatChannelType.BLOCK_COMMAND, "CommandBlock");
    }

    public static void sendChatAsync(String message, ConnJson.ChatChannel.ChatChannelType type, String sender) {
        getScheduler().runDelayedAsync(() -> getClientManager().chat(message, type, sender), 0);
    }

    public static void sendStatsAsync(ConnJson.StatsChannel.StatsChannelEvent type) {
        getScheduler().runDelayedAsync(() -> getClientManager().updateStatsChannel(type), 0);
    }
}
