package me.lianecx.discordlinker.common.abstraction;

import me.lianecx.discordlinker.common.ConnJson;
import me.lianecx.discordlinker.common.network.protocol.payloads.SyncedRoleMemberPayload;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventJsonResponse;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventResponse;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getConnJson;
import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getTeamsAndGroupsBridge;

public interface TeamsAndGroupsBridge {

    /**
     * Starts periodic checking for team and group updates if necessary per implementation.
     */
    default void startTeamCheck() {}

    /**
     * Stops periodic checking for team and group updates if necessary per implementation.
     */
    default void stopTeamCheck() {}

    /**
     * Helper method to get a DiscordEventResponse for a SyncedRoleMemberPayload after adding or removing a member.
     *
     * @param payload The payload containing the role information.
     * @return A CompletableFuture of DiscordEventResponse containing the synced role with updated members or an error response.
     */
    static CompletableFuture<DiscordEventResponse> getRoleResponseFromPayload(SyncedRoleMemberPayload payload) {
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
     * Gets a list of offline players in the specified group or team.
     *
     * @return A Future of UUIDs as LuckPerms or similar may pull data asynchronously from their DB.
     */
    CompletableFuture<List<String>> getPlayersInGroupOrTeam(String groupName, boolean isGroup);

    /**
     * Adds a player to the specified group or team.
     *
     * @param uuid The UUID of the player to add.
     */
    void addPlayerToGroupOrTeam(String name, boolean isGroup, String uuid);

    /**
     * Removes a player from the specified group or team.
     *
     * @param uuid The UUID of the player to remove.
     */
    void removePlayerFromGroupOrTeam(String name, boolean isGroup, String uuid);

    /**
     * Lists all available groups. Empty if LuckPerms or similar is not loaded.
     *
     * @return A list of group names.
     */
    List<String> listGroups();

    /**
     * Lists all available teams.
     *
     * @return A list of team names.
     */
    List<String> listTeams();
}
