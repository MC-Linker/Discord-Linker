package me.lianecx.discordlinker.common.network.client;

import com.google.gson.JsonObject;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventResponse;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public interface DiscordClient {

    void connect(Consumer<Boolean> onReady);

    void disconnect();

    boolean isConnected();

    void send(String event, Object[] payload);

    void send(String event, Object[] payload, Consumer<DiscordEventResponse> callback);

    void on(String event, Function<Object[], DiscordEventResponse> handler);

    void once(String event, Function<Object[], DiscordEventResponse> handler);

    void onAny(BiFunction<String, Object[], DiscordEventResponse> handler);
}
