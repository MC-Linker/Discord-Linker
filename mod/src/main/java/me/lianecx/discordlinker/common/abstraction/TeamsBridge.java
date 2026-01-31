package me.lianecx.discordlinker.common.abstraction;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface TeamsBridge {

    default void startTeamCheck() {
        // Optional method
    }

    default void stopTeamCheck() {
        // Optional method
    }

    CompletableFuture<List<String>> getPlayersInTeam(String teamName);

    void addToTeam(String teamName, String name);

    void removeFromTeam(String teamName, String name);

    List<String> listTeams();
}
