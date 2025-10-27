package me.lianecx.discordlinker.network.adapters;

import java.util.function.Consumer;

public interface NetworkAdapter {

    void disconnect();

    void connect(int httpPort, Consumer<Boolean> callback);
}
