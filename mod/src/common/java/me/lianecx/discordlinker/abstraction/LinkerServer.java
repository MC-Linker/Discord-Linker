package me.lianecx.discordlinker.abstraction;

import java.util.List;

public interface LinkerServer {
    String getServerName();

    int getMaxPlayers();

    int getOnlinePlayersCount();

    List<LinkerPlayer> getOnlinePlayers();

    LinkerPlayer getPlayer(String username);

    void broadcastMessage(String message);
}