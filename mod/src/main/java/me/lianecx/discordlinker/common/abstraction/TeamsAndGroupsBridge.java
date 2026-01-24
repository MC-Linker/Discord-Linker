package me.lianecx.discordlinker.common.abstraction;

import me.lianecx.discordlinker.common.ConnJson;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;

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
     * Gets a list of offline players in the specified group or team.
     * @return A Future as LuckPerms or similar may pull data asynchronously from their DB.
     */
    CompletableFuture<List<LinkerOfflinePlayer>> getPlayersInGroupOrTeam(String groupName, boolean isGroup);

    /**
     * Updates the members of the specified group or team.
     * @param onlyAddMembers If true, only adds members and does not remove any existing members.
     * @param callback A callback that receives the updated list of member UUIDs as strings.
     */
    void updateGroupOrTeamMembers(String groupName, boolean isGroup, boolean onlyAddMembers, Consumer<List<String>> callback);

    /**
     * Adds a player to the specified group or team.
     * @param uuid The UUID of the player to add.
     * @param callback A callback that receives the updated list of member UUIDs as strings.
     */
    void addPlayerToGroupOrTeam(String name, boolean isGroup, String uuid, Consumer<List<String>> callback);

    /**
     * Removes a player from the specified group or team.
     * @param uuid The UUID of the player to remove.
     * @param callback A callback that receives the updated list of member UUIDs as strings.
     */
    void removePlayerFromGroupOrTeam(String name, boolean isGroup, String uuid, Consumer<List<String>> callback);

    /**
     * Lists all available groups. Empty if LuckPerms or similar is not loaded.
     * @return A list of group names.
     */
    List<String> listGroups();

    /**
     * Lists all available teams.
     * @return A list of team names.
     */
    List<String> listTeams();
}
