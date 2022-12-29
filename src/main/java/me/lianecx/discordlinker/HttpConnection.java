package me.lianecx.discordlinker;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import express.http.RequestMethod;
import org.bukkit.ChatColor;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.stream.Collectors;

enum ChatType {
    CHAT,
    JOIN,
    QUIT,
    ADVANCEMENT,
    DEATH,
    PLAYER_COMMAND,
    CONSOLE_COMMAND,
    BLOCK_COMMAND,
    START,
    CLOSE;

    String getKey() {
        return name().toLowerCase();
    }
}

public class HttpConnection {

    private static final String PLUGIN_VERSION = DiscordLinker.getPlugin().getDescription().getVersion();

    //If snapshot version, request test-bot at port 3101 otherwise request main-bot at port 3100
    private static final int BOT_PORT = PLUGIN_VERSION.contains("SNAPSHOT") ? 3101 : 3100;
    private static final String BOT_URL = "http://smpbot.duckdns.org:" + BOT_PORT;

    private static boolean shouldChat() {
        if(DiscordLinker.getConnJson() == null || DiscordLinker.getConnJson().get("channels") == null) return false;
        return DiscordLinker.getConnJson().getAsJsonArray("channels").size() > 0;
    }

    private static JsonArray getChannels(ChatType type) {
        if(!shouldChat()) return null;

        JsonArray allChannels = DiscordLinker.getConnJson().getAsJsonArray("channels");
        JsonArray filteredChannels = new JsonArray();
        for(JsonElement channel : allChannels) {
            try {
                JsonArray types = channel.getAsJsonObject().getAsJsonArray("types");
                if(types.contains(new JsonPrimitive(type.getKey()))) filteredChannels.add(channel);
            }
            catch(Exception err) {
                //If channel is corrupted, remove
                allChannels.remove(channel);

                try {
                    DiscordLinker.getPlugin().updateConn();
                }
                catch(IOException ignored) {}
            }
        }

        return filteredChannels;
    }

    public static void sendChat(String message, ChatType type, String player) {
        JsonArray channels = getChannels(type);
        if(channels == null || channels.size() == 0) return;

        JsonObject chatJson = new JsonObject();
        chatJson.addProperty("type", type.getKey());
        chatJson.addProperty("player", player);
        chatJson.addProperty("message", ChatColor.stripColor(message));
        chatJson.add("channels", channels);
        chatJson.add("id", DiscordLinker.getConnJson().get("id"));
        chatJson.add("ip", DiscordLinker.getConnJson().get("ip"));

        int code = send(RequestMethod.POST, "/chat", chatJson);
        if(code == 403) DiscordLinker.getPlugin().disconnect();
    }

    public static void sendVerificationResponse(String code, UUID uuid) {
        JsonObject verifyJson = new JsonObject();
        verifyJson.addProperty("code", code);
        verifyJson.addProperty("uuid", uuid.toString());

        send(RequestMethod.POST, "/verify/response", verifyJson);
    }

    public static int send(RequestMethod method, String route, JsonObject body) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(BOT_URL + route).openConnection();

            byte[] out = body.toString().getBytes(StandardCharsets.UTF_8);
            int length = out.length;

            conn.setRequestMethod(method.getMethod());
            conn.setRequestProperty("Content-type", "application/json");
            conn.setFixedLengthStreamingMode(length);
            conn.setDoOutput(true);

            conn.connect();
            try(OutputStream os = conn.getOutputStream()) {
                os.write(out);
            }

            return conn.getResponseCode();
        }
        catch(IOException ignored) {}

        return 0;
    }

    public static void checkVersion() {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(BOT_URL + "/version").openConnection();
            InputStream inputStream = conn.getInputStream();
            String latestVersion = new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining("\n"));
            if(!latestVersion.equals(PLUGIN_VERSION))
                DiscordLinker.getPlugin().getLogger().info(ChatColor.AQUA + "Please update to the latest Discord-Linker version (" + latestVersion + ") for a bug-free and feature-rich experience.");

        }
        catch(IOException ignored) {}
    }
}
