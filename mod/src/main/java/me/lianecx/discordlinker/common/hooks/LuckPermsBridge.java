package me.lianecx.discordlinker.common.hooks;

import me.lianecx.discordlinker.common.ConnJson;
import me.lianecx.discordlinker.common.abstraction.LinkerOfflinePlayer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.event.node.NodeMutateEvent;
import net.luckperms.api.event.group.GroupDeleteEvent;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.matcher.NodeMatcher;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.*;

public final class LuckPermsBridge {

    private static final String GROUP_NODE_PREFIX = "group.";

    private final LuckPerms api = LuckPermsProvider.get();

    public LuckPermsBridge() {
        api.getEventBus().subscribe(this, NodeMutateEvent.class, this::onNodeMutate);
        api.getEventBus().subscribe(this, GroupDeleteEvent.class, this::onGroupDelete);
    }

    public boolean hasPermission(LinkerOfflinePlayer player, String permission) {
        User user = api.getUserManager().getUser(UUID.fromString(player.getUUID()));
        if(user == null) return false;
        return user.getCachedData().getPermissionData().checkPermission(permission).asBoolean();
    }

    public CompletableFuture<List<String>> getPlayersInGroup(String group) {
        Group lpGroup = api.getGroupManager().getGroup(group);
        if(lpGroup == null) return CompletableFuture.completedFuture(new ArrayList<>());

        NodeMatcher<Node> matcher = NodeMatcher.key(Node.builder(GROUP_NODE_PREFIX + group).build());
        return api.getUserManager().searchAll(matcher).thenCompose((map) -> {
            List<String> members = map.keySet().stream()
                    .map(UUID::toString)
                    .collect(Collectors.toList());
            return CompletableFuture.completedFuture(members);
        });
    }

    public void addToGroup(String group, String uuid) {
        api.getUserManager().modifyUser(UUID.fromString(uuid), user -> {
            user.data().add(Node.builder(GROUP_NODE_PREFIX + group).build());
        });
    }

    public void removeFromGroup(String group, String uuid) {
        api.getUserManager().modifyUser(UUID.fromString(uuid), user -> {
            user.data().remove(Node.builder(GROUP_NODE_PREFIX + group).build());
        });
    }

    /**
     * Handles LuckPerms node mutation events to detect group membership changes.
     * When a user gains or loses a {@code group.<name>} node, and there is a matching synced role,
     * the bot is notified via add/remove member events.
     */
    private void onNodeMutate(NodeMutateEvent event) {
        if(!(event.getTarget() instanceof User)) return;

        ConnJson conn = getConnJson();
        if(conn == null || conn.getSyncedRoles().isEmpty()) return;

        User user = (User) event.getTarget();
        UUID uuid = user.getUniqueId();

        Set<String> groupsBefore = extractGroupNames(event.getDataBefore());
        Set<String> groupsAfter = extractGroupNames(event.getDataAfter());

        boolean changed = false;

        for(ConnJson.SyncedRole role : conn.getSyncedRoles()) {
            if(!role.isGroup()) continue;

            String groupName = role.getName();
            boolean wasMember = groupsBefore.contains(groupName);
            boolean isMember = groupsAfter.contains(groupName);

            if(!wasMember && isMember) {
                // Player was added to the group
                if(role.syncsToDiscord()) getClientManager().addSyncedRoleMember(groupName, true, uuid);
                if(role.getPlayers() != null && !role.getPlayers().contains(uuid.toString())) {
                    role.getPlayers().add(uuid.toString());
                    changed = true;
                }
            }
            else if(wasMember && !isMember) {
                // Player was removed from the group
                if(role.syncsToDiscord()) getClientManager().removeSyncedRoleMember(groupName, true, uuid);
                if(role.getPlayers() != null) {
                    role.getPlayers().remove(uuid.toString());
                    changed = true;
                }
            }
        }

        if(changed) conn.write();
    }

    /**
     * Handles LuckPerms group deletion events.
     * When a group is deleted and has a corresponding synced role, the bot is notified.
     */
    private void onGroupDelete(GroupDeleteEvent event) {
        ConnJson conn = getConnJson();
        if(conn == null || conn.getSyncedRoles().isEmpty()) return;

        String groupName = event.getGroupName();
        ConnJson.SyncedRole role = conn.getSyncedRole(groupName, true);
        if(role != null) {
            getLogger().warn("LuckPerms group '" + groupName + "' was deleted. Removing synced role.");
            getClientManager().removeSyncedRole(groupName, true);
        }
    }

    /**
     * Extracts group names from a set of LuckPerms nodes (filters for {@code group.<name>} nodes).
     */
    private Set<String> extractGroupNames(Set<Node> nodes) {
        Set<String> groups = new HashSet<>();
        for(Node node : nodes) {
            String key = node.getKey();
            if(key.startsWith(GROUP_NODE_PREFIX)) {
                groups.add(key.substring(GROUP_NODE_PREFIX.length()));
            }
        }
        return groups;
    }

    private List<String> getGroupMembers(String group) {
        return api.getUserManager()
                .getLoadedUsers().stream()
                .filter(u -> u.getNodes().contains(Node.builder(GROUP_NODE_PREFIX + group).build()))
                .map(u -> u.getUniqueId().toString())
                .collect(Collectors.toList());
    }

    public List<String> listGroups() {
        return api.getGroupManager().getLoadedGroups()
                .stream()
                .map(Group::getName)
                .collect(Collectors.toList());
    }
}
