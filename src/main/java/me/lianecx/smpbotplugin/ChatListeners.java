package me.lianecx.smpbotplugin;

import com.google.gson.*;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ChatListeners implements Listener  {
    public static boolean shouldChat() {
        try {
            Reader reader = Files.newBufferedReader(Paths.get(Bukkit.getServer().getPluginManager().getPlugin("SMPBotPlugin").getDataFolder() + "/connection.conn"));
            JsonObject parser = new JsonParser().parse(reader).getAsJsonObject();
            boolean chat = parser.get("chat").getAsBoolean();
            reader.close();
            return chat;
        } catch (IOException err) {
            return false;
        }
    }

    public static void send(String message, int type, String player) {
        if(shouldChat()) {
            try {
                Reader reader = Files.newBufferedReader(Paths.get(Bukkit.getServer().getPluginManager().getPlugin("SMPBotPlugin").getDataFolder() + "/connection.conn"));
                JsonObject parser = new JsonParser().parse(reader).getAsJsonObject();
                JsonArray types = parser.get("types").getAsJsonArray();

                JsonObject typeObject = new JsonObject();
                typeObject.addProperty("type", String.valueOf(type));
                typeObject.addProperty("enabled", true);
                if(!types.contains(typeObject)) return;

                JsonObject chatJson = new JsonObject();
                chatJson.addProperty("type", type);
                chatJson.addProperty("player", player);
                chatJson.addProperty("message", message);
                chatJson.add("channel", parser.get("channel"));
                chatJson.add("guild", parser.get("guild"));
                chatJson.add("ip", parser.get("ip"));

                byte[] out = chatJson.toString().getBytes(StandardCharsets.UTF_8);
                int length = out.length;
                //TODO change IP
                HttpURLConnection conn = (HttpURLConnection) new URL("http://87.161.150.145:3100/chat").openConnection();
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
    }

    @EventHandler
    public void onChatmessage(AsyncPlayerChatEvent event) {
        send(event.getMessage(), 0, event.getPlayer().getDisplayName());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        send(event.getJoinMessage(), 1, event.getPlayer().getDisplayName());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        send(event.getQuitMessage(), 2, event.getPlayer().getDisplayName());
    }

    @EventHandler
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        send(event.getAdvancement().getKey().toString(), 3, event.getPlayer().getDisplayName());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        send(event.getDeathMessage(), 4, event.getEntity().getDisplayName());
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        send(event.getMessage(), 5, event.getPlayer().getDisplayName());
    }
}
