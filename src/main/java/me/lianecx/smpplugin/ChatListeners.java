package me.lianecx.smpplugin;

import com.google.gson.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.server.ServerCommandEvent;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ChatListeners implements Listener  {
    private static final SMPPlugin PLUGIN = SMPPlugin.getPlugin();

    public static boolean shouldChat() {
        if(SMPPlugin.getConnJson() == null || SMPPlugin.getConnJson().get("chat") == null) return false;
        return SMPPlugin.getConnJson().get("chat").getAsBoolean();
    }

    public static void send(String message, int type, String player) {
        if(!shouldChat()) return;

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL("http://smpbot.duckdns.org:3100/chat").openConnection();

            JsonArray types = SMPPlugin.getConnJson().get("types").getAsJsonArray();
            JsonObject typeObject = new JsonObject();
            typeObject.addProperty("type", String.valueOf(type));
            typeObject.addProperty("enabled", true);
            if(!types.contains(typeObject)) return;

            JsonObject chatJson = new JsonObject();
            chatJson.addProperty("type", type);
            chatJson.addProperty("player", player);
            chatJson.addProperty("message", message);
            chatJson.add("channel", SMPPlugin.getConnJson().get("channel"));
            chatJson.add("guild", SMPPlugin.getConnJson().get("guild"));
            chatJson.add("ip", SMPPlugin.getConnJson().get("ip"));

            byte[] out = chatJson.toString().getBytes(StandardCharsets.UTF_8);
            int length = out.length;

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);
            conn.setFixedLengthStreamingMode(length);

            OutputStream outputStream = conn.getOutputStream();
            outputStream.write(out);

            conn.getInputStream();
        } catch(IOException ignored) {}
    }

    @EventHandler
    public void onChatmessage(AsyncPlayerChatEvent event) {
        PLUGIN.getServer().getScheduler().runTaskAsynchronously(PLUGIN, () ->
            send(event.getMessage(), 0, event.getPlayer().getDisplayName()));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        PLUGIN.getServer().getScheduler().runTaskAsynchronously(PLUGIN, () ->
                send(event.getJoinMessage(), 1, event.getPlayer().getDisplayName()));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        PLUGIN.getServer().getScheduler().runTaskAsynchronously(PLUGIN, () ->
            send(event.getQuitMessage(), 2, event.getPlayer().getDisplayName()));
    }

    @EventHandler
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        PLUGIN.getServer().getScheduler().runTaskAsynchronously(PLUGIN, () ->
            send(event.getAdvancement().getKey().toString(), 3, event.getPlayer().getDisplayName()));
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        PLUGIN.getServer().getScheduler().runTaskAsynchronously(PLUGIN, () ->
            send(event.getDeathMessage(), 4, event.getEntity().getDisplayName()));
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        PLUGIN.getServer().getScheduler().runTaskAsynchronously(PLUGIN, () ->
            send(event.getMessage(), 5, event.getPlayer().getDisplayName()));
    }

    @EventHandler
    public void onServerCommand(ServerCommandEvent event) {
        PLUGIN.getServer().getScheduler().runTaskAsynchronously(PLUGIN, () ->
                send(event.getCommand(), 5, event.getSender().getName()));
    }
}
