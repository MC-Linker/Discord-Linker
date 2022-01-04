package me.lianecx.smpplugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class HttpConnection {
    private static final int BOT_PORT = 3100;
    //TODO update version
    private static final String PLUGIN_VERSION = "1.2";

    private static boolean shouldChat() {
        if(SMPPlugin.getConnJson() == null || SMPPlugin.getConnJson().get("chat") == null) return false;
        return SMPPlugin.getConnJson().get("chat").getAsBoolean();
    }

    public static void send(String message, int type, String player) {
        if(!shouldChat()) return;

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL("http://smpbot.duckdns.org:"+BOT_PORT+"/chat").openConnection();

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

    public static void checkVersion() {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL("http://smpbot.duckdns.org:"+BOT_PORT+"/version").openConnection();
            InputStream inputStream = conn.getInputStream();
            String latestVersion = new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining("\n"));
            if(!latestVersion.equals(PLUGIN_VERSION)) SMPPlugin.getPlugin().getLogger().info(ChatColor.AQUA + "Please update to the latest SMP-Plugin version (" + latestVersion + ") for a bug-free and feature-rich experience.");

        } catch (IOException ignored) {}
    }
}
