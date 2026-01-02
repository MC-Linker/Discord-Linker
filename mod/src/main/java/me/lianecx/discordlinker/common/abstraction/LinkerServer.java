package me.lianecx.discordlinker.common.abstraction;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public interface LinkerServer {
    String getServerName();

    int getMaxPlayers();

    int getOnlinePlayersCount();

    List<LinkerPlayer> getOnlinePlayers();

    /**
     * Gets the player by the given UUID, only if they are online.
     * Returns null if the player is not online.
     */
    @Nullable
    LinkerPlayer getPlayer(UUID uuid);

    /**
     * Gets the player by the given name, regardless if they are offline or online.
     * This method may involve a blocking web request to get the UUID for the given name.
     * This will return an object even if the player does not exist. To this method, all players will exist.
     * Only returns null, if an error occurred.
     */
    @Nullable
    LinkerOfflinePlayer getOfflinePlayer(String username);

    /**
     * Gets the player by the given UUID, regardless if they are offline or online.
     * This method may involve a blocking web request to get the UUID for the given name.
     * Returns null if it could not fetch the player's name or if an error occurred.
     */
    @Nullable
    LinkerOfflinePlayer getOfflinePlayer(UUID uuid);

    void broadcastMessage(String message);
}