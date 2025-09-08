package me.lianecx.discordlinker.spigot.network.adapters;

import java.util.function.Consumer;

public interface NetworkAdapter {

    void disconnect();

    void connect(int httpPort, Consumer<Boolean> callback);
}
