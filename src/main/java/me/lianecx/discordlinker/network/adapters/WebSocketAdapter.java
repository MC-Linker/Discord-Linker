package me.lianecx.discordlinker.network.adapters;

import com.google.gson.JsonElement;
import io.socket.client.IO;
import io.socket.client.Socket;
import me.lianecx.discordlinker.DiscordLinker;
import me.lianecx.discordlinker.network.HttpConnection;
import me.lianecx.discordlinker.network.Router;
import org.bukkit.ChatColor;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class WebSocketAdapter {

    private final Socket socket;
    private static final Logger LOGGER = DiscordLinker.getPlugin().getLogger();


    public WebSocketAdapter(Map<String, String> auth) {
        Set<Map.Entry<String, JsonElement>> queries = Router.CONNECT_RESPONSE.entrySet();
        String queryString = queries.stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));

        IO.Options ioOptions = IO.Options.builder()
                .setAuth(auth)
                .setQuery(queryString)
                .setReconnectionDelayMax(10000)
                .build();

        Socket socket = IO.socket(HttpConnection.BOT_URL, ioOptions);

        socket.on(Socket.EVENT_CONNECT_ERROR, args -> LOGGER.info(ChatColor.RED + "Could not reach the Discord Bot! Reconnecting..."));
        socket.on(Socket.EVENT_CONNECT, args -> LOGGER.info(ChatColor.GREEN + "Connected to the Discord Bot!"));
        socket.on(Socket.EVENT_DISCONNECT, args -> {
            if(args[0].equals("io server disconnect")) {
                LOGGER.info(ChatColor.RED + "Disconnected from the Discord Bot!");
                DiscordLinker.getPlugin().disconnect();
            }
        });

        socket.onAnyIncoming(args -> {
            LOGGER.info("Incoming: " + Arrays.toString(args));
        });

        this.socket = socket;
    }

    public void connect() {
        socket.connect();
    }

    public void disconnect() {
        socket.disconnect();
    }
}
