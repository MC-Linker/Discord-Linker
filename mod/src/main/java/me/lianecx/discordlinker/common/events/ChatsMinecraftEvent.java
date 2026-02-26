package me.lianecx.discordlinker.common.events;

import me.lianecx.discordlinker.common.ConnJson;
import me.lianecx.discordlinker.common.events.data.*;
import me.lianecx.discordlinker.common.util.MinecraftChatColor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.*;

public class ChatsMinecraftEvent {

    private static final String CONSOLE_SENDER_NAME = "Server";
    private static final String COMMAND_BLOCK_SENDER_NAME = "CommandBlock";

    public static void handleServerStart(ServerStartEventData event) {
        sendChatAsync("", ConnJson.ChatChannel.ChatChannelType.START, null);
        sendStatsAsync(ConnJson.StatsChannel.StatsChannelEvent.ONLINE);
    }

    public static void handleServerStop(ServerStopEventData event) {
        // Wait briefly wait for acks so stop packets are not dropped before disconnect
        CompletableFuture<Void> closeChat = getClientManager().chatAwaitAck("", ConnJson.ChatChannel.ChatChannelType.CLOSE, null);
        CompletableFuture<Void> statsOffline = getClientManager().updateStatsChannelAwaitAck(ConnJson.StatsChannel.StatsChannelEvent.OFFLINE, 0);
        CompletableFuture<Void> statsMembers = getClientManager().updateStatsChannelAwaitAck(ConnJson.StatsChannel.StatsChannelEvent.MEMBERS, 0);

        try {
            CompletableFuture.allOf(closeChat, statsOffline, statsMembers).get(3, TimeUnit.SECONDS);
        }
        catch(Exception ignored) {}
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
        sendChatAsync(event.command, ConnJson.ChatChannel.ChatChannelType.CONSOLE_COMMAND, CONSOLE_SENDER_NAME);
    }

    public static void handleBlockCommand(BlockCommandEventData event) {
        //TODO name of command block?
        sendChatAsync(event.command, ConnJson.ChatChannel.ChatChannelType.BLOCK_COMMAND, COMMAND_BLOCK_SENDER_NAME);
    }

    public static void sendChatAsync(String message, ConnJson.ChatChannel.ChatChannelType type, String sender) {
        if(getConnJson() == null || getConnJson().getChatChannels().isEmpty()) return;
        getScheduler().runAsync(() -> getClientManager().chat(message, type, sender));
    }

    public static void sendStatsAsync(ConnJson.StatsChannel.StatsChannelEvent type) {
        if(getConnJson() == null || getConnJson().getStatsChannels().isEmpty()) return;
        // Wait a tick to update online players count after join/quit events
        getScheduler().runDelayedAsync(() -> getClientManager().updateStatsChannel(type), 1);
    }
}
