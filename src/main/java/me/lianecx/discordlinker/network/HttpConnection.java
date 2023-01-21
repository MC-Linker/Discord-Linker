package me.lianecx.discordlinker.network;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import express.http.RequestMethod;
import me.lianecx.discordlinker.DiscordLinker;
import org.bukkit.ChatColor;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.stream.Collectors;

public class HttpConnection {

    private static final String PLUGIN_VERSION = DiscordLinker.getPlugin().getDescription().getVersion();

    //If snapshot version, request test-bot at port 3101 otherwise request main-bot at port 3100
    public static final int BOT_PORT = PLUGIN_VERSION.contains("SNAPSHOT") ? 3101 : 3100;
    public static final URI BOT_URL = URI.create("http://79.205.22.76:" + BOT_PORT);

    public static void sendVerificationResponse(String code, UUID uuid) {
        JsonObject verifyJson = new JsonObject();
        verifyJson.addProperty("code", code);
        verifyJson.addProperty("uuid", uuid.toString());

        send(RequestMethod.POST, "/verify/response", verifyJson);
    }

    public static int send(RequestMethod method, String route, JsonElement body) {
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
