package me.lianecx.discordlinker.common.abstraction;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface TeamsBridge {

    /**
     * Gets the players in the specified team.
     * Returns <b>player names</b>, not UUIDs.
     *
     * @return A list of player names in the team, or null if the team does not exist.
     */
    @Nullable List<String> getPlayersInTeam(String teamName);

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
