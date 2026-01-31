package me.lianecx.discordlinker.common;

import me.lianecx.discordlinker.common.abstraction.LinkerServer;
import me.lianecx.discordlinker.common.abstraction.TeamsBridge;
import me.lianecx.discordlinker.common.hooks.LuckPermsBridge;
import me.lianecx.discordlinker.common.network.protocol.payloads.SyncedRoleMemberPayload;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventJsonResponse;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventResponse;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getConnJson;
import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getTeamsAndGroupsBridge;

public final class TeamsAndGroupsBridge {

    private final @Nullable LuckPermsBridge luckPerms;
    private final TeamsBridge teams;

    public TeamsAndGroupsBridge(LinkerServer server, TeamsBridge teamsBridge) {
        this.luckPerms = server.isPluginOrModEnabled("LuckPerms") ? new LuckPermsBridge() : null;
        this.teams = teamsBridge;
    }

    /**
     * Helper method to get a DiscordEventResponse for a SyncedRoleMemberPayload after adding or removing a member.
     *
     * @param payload The payload containing the role information.
     * @return A CompletableFuture of DiscordEventResponse containing the synced role with updated members or an error response.
     */
    public static CompletableFuture<DiscordEventResponse> getRoleResponseFromPayload(SyncedRoleMemberPayload payload) {
        if(getConnJson() == null) return CompletableFuture.completedFuture(DiscordEventJsonResponse.CONN_JSON_MISSING);

        return getTeamsAndGroupsBridge().getPlayersInGroupOrTeam(payload.role.getName(), payload.role.isGroup()).thenApply(players -> {
            if(players == null)
                return payload.role.isGroup() ? DiscordEventJsonResponse.INVALID_GROUP : DiscordEventJsonResponse.INVALID_TEAM;
            ConnJson.SyncedRole role = getConnJson().getSyncedRole(payload.role.getName(), payload.role.isGroup());
            if(role == null) {
                getConnJson().getSyncedRoles().add(payload.role);
                role = payload.role;
            }
            role.setPlayers(players);
            getConnJson().write();
            return DiscordEventJsonResponse.toJson(players);
        });
    }

    /**
     * Starts periodic checking for team and group updates if necessary per implementation.
     */
    public void startTeamCheck() {
        teams.startTeamCheck();
    }

    /**
     * Stops periodic checking for team and group updates if necessary per implementation.
     */
    public void stopTeamCheck() {
        teams.stopTeamCheck();
    }

    /**
     * Gets a list of offline players in the specified group or team. *
     *
     * @return A Future of UUIDs as LuckPerms or similar may pull data asynchronously from their DB.
     */
    public CompletableFuture<List<String>> getPlayersInGroupOrTeam(String name, boolean isGroup) {
        if(isGroup && luckPerms != null) return luckPerms.getPlayersInGroup(name);
        else if(!isGroup) return teams.getPlayersInTeam(name);
        else return CompletableFuture.completedFuture(new ArrayList<>());
    }

    public void addPlayerToGroupOrTeam(String name, boolean isGroup, String uuid) {
        if(isGroup && luckPerms != null) luckPerms.addToGroup(name, uuid);
        else teams.addToTeam(name, uuid);
    }

    public void removePlayerFromGroupOrTeam(String name, boolean isGroup, String uuid) {
        if(isGroup && luckPerms != null) luckPerms.removeFromGroup(name, uuid);
        else teams.removeFromTeam(name, uuid);
    }

    public List<String> listGroups() {
        return luckPerms != null ? luckPerms.listGroups() : new ArrayList<>();
    }

    public List<String> listTeams() {
        return teams.listTeams();
    }
}
