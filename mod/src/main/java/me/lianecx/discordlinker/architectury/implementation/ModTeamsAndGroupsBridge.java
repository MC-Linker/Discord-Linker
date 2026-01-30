package me.lianecx.discordlinker.architectury.implementation;

import me.lianecx.discordlinker.architectury.util.TeamsBridge;
import me.lianecx.discordlinker.common.abstraction.LinkerServer;
import me.lianecx.discordlinker.common.abstraction.TeamsAndGroupsBridge;
import me.lianecx.discordlinker.common.util.LuckPermsUtil;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class ModTeamsAndGroupsBridge implements TeamsAndGroupsBridge {

    private final @Nullable LuckPermsUtil luckPerms;
    private final TeamsBridge teams;

    public ModTeamsAndGroupsBridge(LinkerServer server) {
        this.luckPerms = server.isPluginOrModEnabled("LuckPerms") ? new LuckPermsUtil() : null;
        this.teams = new TeamsBridge(server);
    }

    @Override
    public CompletableFuture<List<String>> getPlayersInGroupOrTeam(String name, boolean isGroup) {
        if(isGroup && luckPerms != null) return luckPerms.getPlayersInGroup(name);
        return teams.getPlayersInTeam(name);
    }

    @Override
    public void addPlayerToGroupOrTeam(String name, boolean isGroup, String uuid) {
        if(isGroup && luckPerms != null) luckPerms.addToGroup(name, uuid);
        else teams.addToTeam(name, uuid);
    }

    @Override
    public void removePlayerFromGroupOrTeam(String name, boolean isGroup, String uuid) {
        if(isGroup && luckPerms != null) luckPerms.removeFromGroup(name, uuid);
        else teams.removeFromTeam(name, uuid);
    }

    @Override
    public List<String> listGroups() {
        return luckPerms != null ? luckPerms.listGroups() : new ArrayList<>();
    }

    @Override
    public List<String> listTeams() {
        return teams.listTeams();
    }
}
