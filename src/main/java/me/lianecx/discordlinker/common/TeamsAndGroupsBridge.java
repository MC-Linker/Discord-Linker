package me.lianecx.discordlinker.common;

import me.lianecx.discordlinker.common.abstraction.LinkerOfflinePlayer;
import me.lianecx.discordlinker.common.abstraction.LinkerServer;
import me.lianecx.discordlinker.common.abstraction.TeamsBridge;
import me.lianecx.discordlinker.common.abstraction.core.LinkerScheduler;
import me.lianecx.discordlinker.common.abstraction.GroupPermissionsBridge;
import me.lianecx.discordlinker.common.hooks.HookLoader;
import me.lianecx.discordlinker.common.hooks.HookProvider;
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
    private final @Nullable GroupPermissionsBridge groupPermissions;
    private final TeamsBridge teams;

    private @Nullable LinkerScheduler.LinkerSchedulerRepeatingTask teamCheckTask;
    private @Nullable LinkerScheduler.LinkerSchedulerRepeatingTask groupCheckTask;

    public TeamsAndGroupsBridge(LinkerServer server, TeamsBridge teamsBridge, HookProvider<? extends GroupPermissionsBridge>[] groupProviders) {
        this.server = server;
        this.groupPermissions = new HookLoader<>(groupProviders).load();
        this.teams = teamsBridge;
    }

    public boolean isGroupPermissionsEnabled() {
        return groupPermissions != null;
    }

    public @Nullable String getGroupPermissionsProviderId() {
        return groupPermissions != null ? groupPermissions.id() : null;
    }

    public boolean hasPermission(LinkerOfflinePlayer player, String permission) {
        if(groupPermissions != null) return groupPermissions.hasPermission(player, permission);
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

    // ── Sync check lifecycle ────────────────────────────────────────────

    public void startTeamCheck() {
        if(teamCheckTask != null) return;

        int intervalTicks = getConfig().getSyncCheckIntervalSeconds() * 20;
        teamCheckTask = getScheduler().runRepeatingAsync(() -> runSyncCheck(false), intervalTicks, intervalTicks);
    }

    public void stopTeamCheck() {
        if(teamCheckTask != null) {
            teamCheckTask.cancel();
            teamCheckTask = null;
        }
    }

    public void startGroupCheck() {
        if(groupCheckTask != null || groupPermissions == null) return;

        int intervalTicks = getConfig().getSyncCheckIntervalSeconds() * 20;
        groupCheckTask = getScheduler().runRepeatingAsync(() -> runSyncCheck(true), intervalTicks, intervalTicks);
    }

    public void stopGroupCheck() {
        if(groupCheckTask != null) {
            groupCheckTask.cancel();
            groupCheckTask = null;
        }
    }

    /**
     * Runs a single membership diff for all synced roles of the given type (group or team).
     * Detects added/removed members and deleted groups/teams, notifying the bot accordingly.
     */
    private void runSyncCheck(boolean isGroup) {
        ConnJson conn = getConnJson();
        if(conn == null) return;

        List<ConnJson.SyncedRole> roles = new ArrayList<>(conn.getSyncedRoles());

        boolean changed = false;
        for(ConnJson.SyncedRole role : roles) {
            if(role.isGroup() != isGroup) continue;

            String name = role.getName();

            // We can block here since we're already async
            List<String> currentPlayers = getPlayersInGroupOrTeam(name, isGroup).join();

            if(currentPlayers == null) {
                // Group/team was deleted
                String type = isGroup ? "Group" : "Team";
                getLogger().debug(MinecraftChatColor.RED + type + " '" + name + "' no longer exists. Removing synced role.");
                getClientManager().removeSyncedRole(name, isGroup);
                changed = true;
                continue;
            }

            List<String> storedPlayers = new ArrayList<>(role.getPlayers());

            Set<String> storedSet = new HashSet<>(storedPlayers);
            Set<String> currentSet = new HashSet<>(currentPlayers);

            if(role.syncsToDiscord()) {
                // MC is authoritative — notify bot of changes
                for(String uuid : currentPlayers) {
                    if(!storedSet.contains(uuid)) {
                        getClientManager().addSyncedRoleMember(name, isGroup, UUID.fromString(uuid));
                        role.getPlayers().add(uuid);
                        changed = true;
                    }
                }

                for(String uuid : storedPlayers) {
                    if(!currentSet.contains(uuid)) {
                        getClientManager().removeSyncedRoleMember(name, isGroup, UUID.fromString(uuid));
                        role.getPlayers().remove(uuid);
                        changed = true;
                    }
                }
            }
            else {
                // Discord is authoritative — revert any MC-side changes
                for(String uuid : currentPlayers) {
                    if(!storedSet.contains(uuid)) {
                        removePlayerFromGroupOrTeam(name, isGroup, uuid);
                    }
                }

                for(String uuid : storedPlayers) {
                    if(!currentSet.contains(uuid)) {
                        addPlayerToGroupOrTeam(name, isGroup, uuid);
                    }
                }
            }
        }

        if(changed) conn.write();
    }

    // ── Group/team operations ───────────────────────────────────────────

    /**
     * Gets a list of player UUIDs in the specified group or team.
     * For groups, UUIDs are returned directly from the permissions provider.
     * For teams, player names are resolved to UUIDs via the profile cache.
     *
     * @return A Future of UUIDs, or a future of {@code null} if the group/team does not exist.
     */
    public CompletableFuture<List<String>> getPlayersInGroupOrTeam(String name, boolean isGroup) {
        if(isGroup && groupPermissions != null) return groupPermissions.getPlayersInGroup(name);
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
     * For groups, the UUID is passed directly to the permissions provider.
     * For teams, the UUID is resolved to a player name first.
     */
    public CompletableFuture<Void> addPlayerToGroupOrTeam(String name, boolean isGroup, String uuid) {
        getLogger().debug(MinecraftChatColor.GREEN + "Adding player with UUID '" + uuid + "' to " + (isGroup ? "group" : "team") + " '" + name + "'");

        if(isGroup && groupPermissions != null) return groupPermissions.addToGroup(name, uuid);
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
     * For groups, the UUID is passed directly to the permissions provider.
     * For teams, the UUID is resolved to a player name first.
     */
    public CompletableFuture<Void> removePlayerFromGroupOrTeam(String name, boolean isGroup, String uuid) {
        getLogger().debug(MinecraftChatColor.RED + "Removing player with UUID '" + uuid + "' from " + (isGroup ? "group" : "team") + " '" + name + "'");

        if(isGroup && groupPermissions != null) return groupPermissions.removeFromGroup(name, uuid);
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

    // ── Helpers ─────────────────────────────────────────────────────────

    private @Nullable String resolveNameToUUID(String playerName) {
        LinkerOfflinePlayer player = server.getOfflinePlayer(playerName);
        return player != null ? player.getUUID() : null;
    }

    private @Nullable String resolveUUIDToName(String uuid) {
        LinkerOfflinePlayer player = server.getOfflinePlayer(UUID.fromString(uuid));
        return player != null ? player.getName() : null;
    }

    public List<String> listGroups() {
        return groupPermissions != null ? groupPermissions.listGroups() : new ArrayList<>();
    }

    public List<String> listTeams() {
        return teams.listTeams();
    }
}
