package me.lianecx.discordlinker.network.adapters;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import express.http.RequestMethod;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import me.lianecx.discordlinker.DiscordLinker;
import me.lianecx.discordlinker.network.ChatType;
import org.bukkit.ChatColor;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class AdapterManager {

    private static final DiscordLinker PLUGIN = DiscordLinker.getPlugin();
    private int httpPort;
    private HttpAdapter httpAdapter;
    private WebSocketAdapter webSocketAdapter;

    public AdapterManager(String token, int httpPort) {
        this.httpPort = httpPort;
        webSocketAdapter = new WebSocketAdapter(Collections.singletonMap("token", token));
    }

    public AdapterManager(int httpPort) {
        this.httpPort = httpPort;
        httpAdapter = new HttpAdapter();
    }

    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    public void startAll() {
        // If adapters are already connected, disconnect them
        if(isWebSocketConnected()) webSocketAdapter.disconnect();
        if(isHttpConnected()) httpAdapter.disconnect();

        if(webSocketAdapter != null) webSocketAdapter.connect();
        else if(httpAdapter != null) httpAdapter.connect(httpPort);
    }

    public void stopAll() {
        if(isWebSocketConnected()) webSocketAdapter.disconnect();
        if(isHttpConnected()) httpAdapter.disconnect();
    }

    public void startHttp() {
        if(isHttpConnected()) httpAdapter.disconnect();
        else httpAdapter = new HttpAdapter();
        httpAdapter.connect(httpPort);

        webSocketAdapter = null;
    }

    public void stopHttp() {
        if(isHttpConnected()) httpAdapter.disconnect();
        httpAdapter = null;
    }

    /**
     * Connects to the websocket server with a verification code.
     *
     * @param code     The verification code to connect with.
     * @param callback The callback to run when the connection is established or fails.
     */
    public void connectWebsocket(String code, Consumer<Boolean> callback) {
        if(isWebSocketConnected()) webSocketAdapter.disconnect();

        //Create random 32-character hex string
        String token = new BigInteger(130, new SecureRandom()).toString(16);
        Map<String, String> auth = new HashMap<>();
        auth.put("code", code);
        auth.put("token", token);

        WebSocketAdapter tempAdapter = new WebSocketAdapter(auth);

        //Register connection listeners
        Emitter.Listener failListener = objects -> {
            tempAdapter.disconnect();
            callback.accept(false);
            if(webSocketAdapter != null) webSocketAdapter.connect();
        };
        AtomicReference<Emitter.Listener> successListener = new AtomicReference<>();
        successListener.set(objects -> {
            //Remove listeners
            tempAdapter.getSocket().off(Socket.EVENT_DISCONNECT, failListener);
            tempAdapter.getSocket().off(Socket.EVENT_CONNECT_ERROR, failListener);
            tempAdapter.getSocket().off(Socket.EVENT_CONNECT, successListener.get());

            //Code is valid, set the adapter to the new one
            webSocketAdapter = tempAdapter;

            //Save connection data
            JsonObject connJson = new JsonObject();
            connJson.addProperty("protocol", "websocket");
            connJson.addProperty("id", code.split(":")[0]);
            connJson.addProperty("token", token);
            connJson.add("channels", new JsonArray());

            try {
                PLUGIN.updateConn(connJson);
                callback.accept(true);
            }
            catch(IOException e) {
                PLUGIN.getLogger().info(ChatColor.RED + "Failed to save connection data.");
                e.printStackTrace();

                webSocketAdapter.disconnect();
                startHttp();
                callback.accept(false);
            }
        });

        //Set listeners
        tempAdapter.getSocket().on(Socket.EVENT_DISCONNECT, failListener);
        tempAdapter.getSocket().on(Socket.EVENT_CONNECT_ERROR, failListener);
        tempAdapter.getSocket().on("auth-success", successListener.get());

        tempAdapter.connect();
    }

    public boolean isWebSocketConnected() {
        return webSocketAdapter != null && webSocketAdapter.getSocket().connected();
    }

    public boolean isHttpConnected() {
        return httpAdapter != null;
    }

    public void sendChat(String message, ChatType type, String player) {
        JsonArray channels = PLUGIN.filterChannels(type);
        if(channels == null || channels.size() == 0) return;

        JsonObject chatJson = new JsonObject();
        chatJson.addProperty("type", type.getKey());
        chatJson.addProperty("player", player);
        chatJson.addProperty("message", ChatColor.stripColor(message));
        chatJson.add("channels", channels);
        chatJson.add("id", DiscordLinker.getConnJson().get("id"));

        if(isWebSocketConnected()) webSocketAdapter.send("chat", chatJson);
        else if(isHttpConnected()) {
            chatJson.add("ip", DiscordLinker.getConnJson().get("ip"));
            int code = HttpAdapter.send(RequestMethod.POST, "/chat", chatJson);
            if(code == 403) PLUGIN.deleteConn(); //Bot could not find a valid connection to this server
        }
    }
}
