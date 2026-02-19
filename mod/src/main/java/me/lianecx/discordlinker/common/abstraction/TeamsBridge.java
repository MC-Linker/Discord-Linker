package me.lianecx.discordlinker.common.abstraction;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface TeamsBridge {

    /**
     * Gets the players in the specified team.
     * Returns <b>player names</b>, not UUIDs.
     *
     * @return A CompletableFuture of player names, or a future of {@code null} if the team does not exist.
     */
    CompletableFuture<List<String>> getPlayersInTeam(String teamName);

    /**
     * Adds a player to the specified team by their <b>player name</b>.
     */
    void addToTeam(String teamName, String playerName);

    /**
     * Removes a player from the specified team by their <b>player name</b>.
     */
    void removeFromTeam(String teamName, String playerName);

    List<String> listTeams();
}
