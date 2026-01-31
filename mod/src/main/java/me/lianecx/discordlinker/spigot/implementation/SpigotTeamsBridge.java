package me.lianecx.discordlinker.spigot.implementation;

import me.lianecx.discordlinker.common.abstraction.TeamsBridge;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class SpigotTeamsBridge implements TeamsBridge {

    private Scoreboard scoreboard() {
        return Bukkit.getScoreboardManager().getMainScoreboard();
    }

    @Override
    public CompletableFuture<List<String>> getPlayersInTeam(String teamName) {
        Team team = scoreboard().getTeam(teamName);
        if(team == null) return CompletableFuture.completedFuture(null);
        return CompletableFuture.completedFuture(new ArrayList<>(team.getEntries()));
    }

    @Override
    public void addToTeam(String teamName, String entry) {
        Team team = scoreboard().getTeam(teamName);
        if(team != null) team.addEntry(entry);
    }

    @Override
    public void removeFromTeam(String teamName, String entry) {
        Team team = scoreboard().getTeam(teamName);
        if(team != null) team.removeEntry(entry);
    }

    @Override
    public List<String> listTeams() {
        return scoreboard().getTeams().stream()
                .map(Team::getName)
                .collect(Collectors.toList());
    }
}
