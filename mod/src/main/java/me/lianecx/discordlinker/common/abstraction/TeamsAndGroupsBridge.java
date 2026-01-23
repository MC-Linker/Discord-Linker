package me.lianecx.discordlinker.common.abstraction;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public interface TeamsAndGroupsBridge {

    /**
     * Starts periodic checking for team and group updates if necessary per implementation.
     */
    default void startTeamCheck() {

    }

    /**
     * Stops periodic checking for team and group updates if necessary per implementation.
     */
    default void stopTeamCheck() {

    }

    /**
     * Gets a list of offline players in the specified group or team.
     * @return A Future as LuckPerms or similar may pull data asynchronously from their DB.
     */
    CompletableFuture<List<LinkerOfflinePlayer>> getPlayersInGroupOrTeam(String groupName, boolean isGroup);
}
