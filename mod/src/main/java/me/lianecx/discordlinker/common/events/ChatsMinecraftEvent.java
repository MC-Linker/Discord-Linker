package me.lianecx.discordlinker.common.events;

import me.lianecx.discordlinker.common.ConnJson;
import me.lianecx.discordlinker.common.abstraction.core.LinkerScheduler;
import me.lianecx.discordlinker.common.events.data.*;
import me.lianecx.discordlinker.common.util.MinecraftChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.*;

public class ChatsMinecraftEvent {

    private static final String CONSOLE_SENDER_NAME = "Server";
    private static final String COMMAND_BLOCK_SENDER_NAME = "CommandBlock";

    private static final Object CHAT_CONSOLE_LOCK = new Object();

    private static final StringBuilder chatConsoleBuffer = new StringBuilder();
    private static LinkerScheduler.LinkerSchedulerRepeatingTask chatConsoleFlushTask;
    private static int chatConsoleMaxChars = 2000;

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

    public static void startChatConsoleForwarding() {
        synchronized(CHAT_CONSOLE_LOCK) {
            if(chatConsoleFlushTask != null) return;

            // Clamp max chars between 100 and 2000
            chatConsoleMaxChars = Math.min(Math.max(getConfig().getChatConsoleMaxChars(), 100), 2000);

            int flushSeconds = Math.max(getConfig().getChatConsoleFlushSeconds(), 1);
            int flushTicks = flushSeconds * 20;
            chatConsoleFlushTask = getScheduler().runRepeatingAsync(ChatsMinecraftEvent::flushChatConsoleBuffer, flushTicks, flushTicks);
        }
    }

    public static void stopChatConsoleForwarding() {
        synchronized(CHAT_CONSOLE_LOCK) {
            if(chatConsoleFlushTask != null) {
                chatConsoleFlushTask.cancel();
                chatConsoleFlushTask = null;
            }
        }
        flushChatConsoleBuffer();
    }

    public static void handleChatConsoleLine(String line) {
        if(line == null) return;

        List<String> chunksToSend = null;
        synchronized(CHAT_CONSOLE_LOCK) {
            if(chatConsoleFlushTask == null) return;
            if(chatConsoleBuffer.length() > 0) chatConsoleBuffer.append('\n');
            chatConsoleBuffer.append(line);
            if(chatConsoleBuffer.length() >= chatConsoleMaxChars)
                chunksToSend = drainChunks(chatConsoleBuffer, chatConsoleMaxChars);
        }

        // If exceeds max sizze, send immediately, otherwise wait for flush task to send
        sendChatConsoleChunks(chunksToSend);
    }

    private static void flushChatConsoleBuffer() {
        List<String> chunksToSend;
        synchronized(CHAT_CONSOLE_LOCK) {
            if(chatConsoleBuffer.length() == 0) return;
            chunksToSend = drainChunks(chatConsoleBuffer, chatConsoleMaxChars);
        }

        sendChatConsoleChunks(chunksToSend);
    }

    /**
     * Drains the source StringBuilder into a list of chunks of at most chunkSize characters clearing the source buffer.
     */
    private static List<String> drainChunks(StringBuilder source, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        while(source.length() > 0) {
            int end = Math.min(source.length(), chunkSize);
            chunks.add(source.substring(0, end));
            source.delete(0, end);
        }
        return chunks;
    }

    private static void sendChatConsoleChunks(List<String> chunks) {
        if(chunks == null || chunks.isEmpty()) return;
        for(String chunk : chunks)
            sendChatAsync(chunk, ConnJson.ChatChannel.ChatChannelType.CONSOLE, CONSOLE_SENDER_NAME);
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
