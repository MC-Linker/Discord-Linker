package me.lianecx.discordlinker;

import com.google.gson.*;
import org.bukkit.ChatColor;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class HttpConnection {
    private static final int BOT_PORT = 3100;
    private static final String BOT_URL = "http://smpbot.duckdns.org:" + BOT_PORT;
    private static final String PLUGIN_VERSION = DiscordLinker.getPlugin().getDescription().getVersion();

    private static boolean shouldChat() {
        if(DiscordLinker.getConnJson() == null || DiscordLinker.getConnJson().get("channels") == null) return false;
        return DiscordLinker.getConnJson().get("channels").getAsJsonArray().size() > 0;
    }

    private static JsonArray getChannels(String type) {
        if(!shouldChat()) return null;

        JsonArray allChannels = DiscordLinker.getConnJson().getAsJsonArray("channels");
        JsonArray filteredChannels = new JsonArray();
        for (JsonElement channel : allChannels) {
            try {
                JsonArray types = channel.getAsJsonObject().getAsJsonArray("types");
                if(types.contains(new JsonPrimitive(type))) filteredChannels.add(channel);
            } catch(Exception err) {
                //If channel is corrupted, remove
                allChannels.remove(channel);

                try {
                    DiscordLinker.getPlugin().updateConn();
                } catch (IOException ignored) {}
            }
        }

        return filteredChannels;
    }

    public static void send(String message, String type, String player) {
        JsonArray channels = getChannels(type);
        if(channels == null || channels.size() == 0) return;

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(BOT_URL + "/chat").openConnection();

            JsonObject chatJson = new JsonObject();
            chatJson.addProperty("type", type);
            chatJson.addProperty("player", player);
            chatJson.addProperty("message", ChatColor.stripColor(message));
            chatJson.add("channels", channels);
            chatJson.add("id", DiscordLinker.getConnJson().get("id"));
            chatJson.add("ip", DiscordLinker.getConnJson().get("ip"));

            byte[] out = chatJson.toString().getBytes(StandardCharsets.UTF_8);
            int length = out.length;

            conn.setRequestMethod("POST");
            conn.setFixedLengthStreamingMode(length);

            OutputStream outputStream = conn.getOutputStream();
            outputStream.write(out);

            if(conn.getResponseCode() == 403) DiscordLinker.getPlugin().disconnect();
        } catch(IOException ignored) {}
    }

    public static void checkVersion() {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(BOT_URL + "/version").openConnection();
            InputStream inputStream = conn.getInputStream();
            String latestVersion = new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining("\n"));
            if(!latestVersion.equals(PLUGIN_VERSION)) DiscordLinker.getPlugin().getLogger().info(ChatColor.AQUA + "Please update to the latest Discord-Linker version (" + latestVersion + ") for a bug-free and feature-rich experience.");

        } catch (IOException ignored) {}
    }
}
