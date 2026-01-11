package me.lianecx.discordlinker.common;

import me.lianecx.discordlinker.common.abstraction.ConnJson;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class ClientManager {

    private final String token;

    public ClientManager(@Nullable String token) {
        this.token = token;
    }

    /**
     * Called on initialization to check version compatibility.
     */
    public static void checkVersion() {
        // implement version checking logic here if needed
        // for now, this could just log or throw if incompatible
    }

    /**
     * Starts the client connection. Calls the consumer once connected or failed.
     */
    public void start(Consumer<Boolean> connected) {
        // stub: simulate connecting
        // in real code: connect to Discord bot over WebSocket
        connected.accept(true);
    }

    /**
     * Sends a chat message to Discord via the connection.
     */
    public void chat(String message, ConnJson.ChatChannel.ChatChannelType type, @Nullable Object extra) {
        // stub: send the chat message
        // `extra` can be any additional data, e.g., Minecraft player info
    }

    /**
     * Updates the stats channel in Discord.
     */
    public void updateStatsChannel(ConnJson.StatsChannel.StatChannelEvent event) {
        // stub: update stats channel
    }

    /**
     * Stops the client.
     */
    public void stop() {
        // close connection gracefully
    }

    /**
     * Forcefully disconnects the client (used for deprecated connections or errors)
     */
    public void disconnectForce() {
        // forcibly close connection
    }

    public boolean isRunning() {
        return false;
    }

    public String getToken() {
        return token;
    }
}
