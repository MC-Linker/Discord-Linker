package me.lianecx.discordlinker.common;

import me.lianecx.discordlinker.common.abstraction.LinkerOfflinePlayer;
import me.lianecx.discordlinker.common.abstraction.LinkerServer;
import me.lianecx.discordlinker.common.abstraction.TeamsBridge;
import me.lianecx.discordlinker.common.abstraction.core.LinkerScheduler;
import me.lianecx.discordlinker.common.hooks.LuckPermsBridge;
import me.lianecx.discordlinker.common.network.protocol.payloads.SyncedRoleMemberPayload;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventResponse;
import me.lianecx.discordlinker.common.util.MinecraftChatColor;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.*;

public final class TeamsAndGroupsBridge {

    private final LinkerServer server;
    private final @Nullable LuckPermsBridge luckPerms;
    private final TeamsBridge teams;

    private @Nullable LinkerScheduler.LinkerSchedulerRepeatingTask teamCheckTask;

    public TeamsAndGroupsBridge(LinkerServer server, TeamsBridge teamsBridge) {
        this.server = server;
        this.luckPerms = LuckPermsBridge.getSafely();
        this.teams = teamsBridge;
    }

    public boolean isLuckPermsEnabled() {
        return luckPerms != null;
    }

    public boolean hasPermission(LinkerOfflinePlayer player, String permission) {
        if(luckPerms != null) return luckPerms.hasPermission(player, permission);
        return false;
    }

    /**
     * Helper method to get a DiscordEventResponse for a SyncedRoleMemberPayload after adding or removing a member.
     *
     * @param payload The payload containing the role information.
     * @return A CompletableFuture of DiscordEventResponse containing the synced role with updated members or an error response.
     */
    public static CompletableFuture<DiscordEventResponse> getRoleResponseFromPayload(SyncedRoleMemberPayload payload) {
        if(getConnJson() == null) return CompletableFuture.completedFuture(DiscordEventResponse.CONN_JSON_MISSING);

        return getTeamsAndGroupsBridge().getPlayersInGroupOrTeam(payload.role.getName(), payload.role.isGroup())
                .thenApply(players -> {
                    System.out.println(MinecraftChatColor.YELLOW + "Updating synced role '" + payload.role.getName() + "' with players: " + players);
                    if(players == null) return DiscordEventResponse.NOT_FOUND;

                    ConnJson.SyncedRole role = getConnJson().getSyncedRole(payload.role.getName(), payload.role.isGroup());
                    if(role == null) {
                        getConnJson().getSyncedRoles().add(payload.role);
                        role = payload.role;
                    }
                    role.setPlayers(players);
                    getConnJson().write();
                    return DiscordEventResponse.toJson(players);
                });
    }

    /**
     * Starts periodic checking for team membership changes.
     * Compares current team members against stored {@link ConnJson.SyncedRole#getPlayers()} and
     * sends add/remove member events to the bot for any differences.
     */
    public void startTeamCheck() {
        if(teamCheckTask != null) return;

        int intervalSeconds = getConfig().getTeamCheckIntervalSeconds();
        int intervalTicks = intervalSeconds * 20;
        teamCheckTask = getScheduler().runRepeatingAsync(this::runTeamCheck, intervalTicks, intervalTicks);
    }

    /**
     * Stops the periodic team membership check.
     */
    public void stopTeamCheck() {
        if(teamCheckTask != null) {
            teamCheckTask.cancel();
            teamCheckTask = null;
        }
    }

    /**
     * Runs a single team membership diff for all team-based synced roles.
     * Detects added/removed members and deleted teams, notifying the bot accordingly.
     */
    private void runTeamCheck() {
        ConnJson conn = getConnJson();
        if(conn == null) return;

        // Snapshot the roles to avoid concurrent modification
        List<ConnJson.SyncedRole> roles = new ArrayList<>(conn.getSyncedRoles());

        boolean changed = false;
        for(ConnJson.SyncedRole role : roles) {
            if(role.isGroup()) continue; // Only check teams, not LuckPerms groups

            // We can block here since we're already async
            List<String> currentPlayers = getPlayersInGroupOrTeam(role.getName(), false).join();

            if(currentPlayers == null) {
                // Team was deleted
                getLogger().debug(MinecraftChatColor.RED + "Team '" + role.getName() + "' no longer exists. Removing synced role.");
                getClientManager().removeSyncedRole(role.getName(), false);
                changed = true;
                continue;
            }

            List<String> storedPlayers = role.getPlayers();

            // Find added players (in current but not in stored)
            Set<String> storedSet = new HashSet<>(storedPlayers);
            Set<String> currentSet = new HashSet<>(currentPlayers);

            if(role.syncsToDiscord()) {
                for(String uuid : currentPlayers) {
                    if(!storedSet.contains(uuid)) {
                        getClientManager().addSyncedRoleMember(role.getName(), false, UUID.fromString(uuid));
                    }
                }

                for(String uuid : storedPlayers) {
                    if(!currentSet.contains(uuid)) {
                        getClientManager().removeSyncedRoleMember(role.getName(), false, UUID.fromString(uuid));
                    }
                }
            }
            else {
                // If Discord is authoritative, cancel any changes

                for(String uuid : currentPlayers) {
                    if(!storedSet.contains(uuid)) {
                        getScheduler().runSync(() -> teams.removeFromTeam(role.getName(), resolveUUIDToName(uuid)));
                    }
                }

                for(String uuid : storedPlayers) {
                    if(!currentSet.contains(uuid)) {
                        getScheduler().runSync(() -> teams.addToTeam(role.getName(), resolveUUIDToName(uuid)));
                    }
                }
            }
        }

        if(changed) conn.write();
    }

    /**
     * Gets a list of player UUIDs in the specified group or team.
     * For LuckPerms groups, UUIDs are returned directly.
     * For teams, player names are resolved to UUIDs via the profile cache.
     *
     * @return A Future of UUIDs, or a future of {@code null} if the group/team does not exist.
     */
    public CompletableFuture<List<String>> getPlayersInGroupOrTeam(String name, boolean isGroup) {
        if(isGroup && luckPerms != null) return luckPerms.getPlayersInGroup(name);
        else if(!isGroup) {
            List<String> players = teams.getPlayersInTeam(name);
            if(players == null) return CompletableFuture.completedFuture(null);
            return players.stream()
                    .map(this::resolveNameToUUID)
                    .filter(Objects::nonNull)
                    .collect(Collectors.collectingAndThen(Collectors.toList(), CompletableFuture::completedFuture));
        }
        else return CompletableFuture.completedFuture(new ArrayList<>());
    }

    /**
     * Adds a player to a group or team by UUID.
     * For LuckPerms groups, the UUID is passed directly.
     * For teams, the UUID is resolved to a player name first.
     */
    public CompletableFuture<Void> addPlayerToGroupOrTeam(String name, boolean isGroup, String uuid) {
        getLogger().debug(MinecraftChatColor.GREEN + "Adding player with UUID '" + uuid + "' to " + (isGroup ? "group" : "team") + " '" + name + "'");

        if(isGroup && luckPerms != null) return luckPerms.addToGroup(name, uuid);
        else if(!isGroup) {
            String playerName = resolveUUIDToName(uuid);
            if(playerName != null) {
                CompletableFuture<Void> future = new CompletableFuture<>();
                getScheduler().runSync(() -> {
                    teams.addToTeam(name, playerName);
                    future.complete(null);
                });
                return future;
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Removes a player from a group or team by UUID.
     * For LuckPerms groups, the UUID is passed directly.
     * For teams, the UUID is resolved to a player name first.
     */
    public CompletableFuture<Void> removePlayerFromGroupOrTeam(String name, boolean isGroup, String uuid) {
        getLogger().debug(MinecraftChatColor.RED + "Removing player with UUID '" + uuid + "' from " + (isGroup ? "group" : "team") + " '" + name + "'");

        if(isGroup && luckPerms != null) return luckPerms.removeFromGroup(name, uuid);
        else if(!isGroup) {
            String playerName = resolveUUIDToName(uuid);
            if(playerName != null) {
                CompletableFuture<Void> future = new CompletableFuture<>();
                getScheduler().runSync(() -> {
                    teams.removeFromTeam(name, playerName);
                    future.complete(null);
                });
                return future;
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Resolves a player name to a UUID string using the server's profile cache.
     * Returns null if the player cannot be resolved.
     */
    private @Nullable String resolveNameToUUID(String playerName) {
        LinkerOfflinePlayer player = server.getOfflinePlayer(playerName);
        return player != null ? player.getUUID() : null;
    }

    /**
     * Resolves a UUID string to a player name using the server's profile cache.
     * Returns null if the player cannot be resolved.
     */
    private @Nullable String resolveUUIDToName(String uuid) {
        LinkerOfflinePlayer player = server.getOfflinePlayer(UUID.fromString(uuid));
        return player != null ? player.getName() : null;
    }

    public List<String> listGroups() {
        return luckPerms != null ? luckPerms.listGroups() : new ArrayList<>();
    }

    public List<String> listTeams() {
        return teams.listTeams();
    }
}
