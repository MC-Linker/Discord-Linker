package me.lianecx.discordlinker.architectury.util;

import dev.architectury.utils.GameInstance;
import me.lianecx.discordlinker.common.abstraction.LinkerServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class TeamsBridge {

    private final LinkerServer linkerServer;
    private final MinecraftServer server;

    public TeamsBridge(LinkerServer linkerServer) {
        this.linkerServer = linkerServer;
        this.server = GameInstance.getServer();
    }

    public CompletableFuture<List<String>> getPlayersInTeam(String teamName) {
        Team team = getTeam(teamName);
        return CompletableFuture.completedFuture(new ArrayList<>(team.getPlayers()));
    }

    public void addToTeam(String teamName, String name) {
        server.getScoreboard().addPlayerToTeam(name, getTeam(teamName));
    }

    public void removeFromTeam(String teamName, String name) {
        server.getScoreboard().removePlayerFromTeam(name, getTeam(teamName));
    }

    public List<String> listTeams() {
        return new ArrayList<>(server.getScoreboard().getTeamNames());
    }

    private PlayerTeam getTeam(String teamName) {
        Scoreboard scoreboard = server.getScoreboard();
        return scoreboard.getPlayerTeam(teamName);
    }
}
