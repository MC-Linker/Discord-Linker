package me.lianecx.smpplugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.bukkit.ChatColor;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class HttpConnection {
    private static final int BOT_PORT = 3100;
    private static final String PLUGIN_VERSION = SMPPlugin.getPlugin().getDescription().getVersion();

    private static boolean shouldChat() {
        if(SMPPlugin.getConnJson() == null || SMPPlugin.getConnJson().get("chat") == null) return false;
        return SMPPlugin.getConnJson().get("chat").getAsBoolean();
    }

    private static JsonArray getChannels(String type) {
        if(!shouldChat()) return null;

        JsonArray channels = new JsonArray();
        for (JsonElement channel : SMPPlugin.getConnJson().getAsJsonArray("channels")) {
            JsonArray types = channel.getAsJsonObject().getAsJsonArray("types");
            if(types.contains(new JsonPrimitive(type))) channels.add(channel);
        }

        return channels;
    }

    public static void send(String message, String type, String player) {
        JsonArray channels = getChannels(type);
        if(channels == null || channels.size() == 0) return;

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL("http://smpbot.duckdns.org:"+BOT_PORT+"/chat").openConnection();

            JsonObject chatJson = new JsonObject();
            chatJson.addProperty("type", type);
            chatJson.addProperty("player", player);
            chatJson.addProperty("message", ChatColor.stripColor(message));
            chatJson.add("channels", channels);
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

    public static void checkVersion() {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL("http://smpbot.duckdns.org:"+BOT_PORT+"/version").openConnection();
            InputStream inputStream = conn.getInputStream();
            String latestVersion = new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining("\n"));
            if(!latestVersion.equals(PLUGIN_VERSION)) SMPPlugin.getPlugin().getLogger().info(ChatColor.AQUA + "Please update to the latest SMP-Plugin version (" + latestVersion + ") for a bug-free and feature-rich experience.");

        } catch (IOException ignored) {}
    }
}
