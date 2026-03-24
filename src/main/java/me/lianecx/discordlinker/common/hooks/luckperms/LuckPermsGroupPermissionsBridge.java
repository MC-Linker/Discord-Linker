package me.lianecx.discordlinker.common.hooks.luckperms;

import me.lianecx.discordlinker.common.ConnJson;
import me.lianecx.discordlinker.common.abstraction.LinkerOfflinePlayer;
import me.lianecx.discordlinker.common.abstraction.GroupPermissionsBridge;
import me.lianecx.discordlinker.common.util.MinecraftChatColor;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.event.group.GroupDeleteEvent;
import net.luckperms.api.event.node.NodeMutateEvent;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.matcher.NodeMatcher;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getClientManager;
import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getConnJson;
import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getLogger;

public final class LuckPermsGroupPermissionsBridge implements GroupPermissionsBridge {

    private static final String GROUP_NODE_PREFIX = "group.";

    private final LuckPerms api;

    public LuckPermsGroupPermissionsBridge(LuckPerms api) {
        this.api = api;
        api.getEventBus().subscribe(NodeMutateEvent.class, this::onNodeMutate);
        api.getEventBus().subscribe(GroupDeleteEvent.class, this::onGroupDelete);
    }

    public String id() {
        return "luckperms";
    }

    @Override
    public boolean hasPermission(LinkerOfflinePlayer player, String permission) {
        User user = api.getUserManager().getUser(UUID.fromString(player.getUUID()));
        if(user == null) return false;
        return user.getCachedData().getPermissionData().checkPermission(permission).asBoolean();
    }

    @Override
    public CompletableFuture<List<String>> getPlayersInGroup(String group) {
        Group lpGroup = api.getGroupManager().getGroup(group);
        if(lpGroup == null) return CompletableFuture.completedFuture(null);

        NodeMatcher<Node> matcher = NodeMatcher.key(Node.builder(GROUP_NODE_PREFIX + group).build());
        return api.getUserManager().searchAll(matcher).thenApply(map -> map.keySet().stream()
                .map(UUID::toString)
                .collect(Collectors.toList()));
    }

    @Override
    public CompletableFuture<Void> addToGroup(String group, String uuid) {
        return api.getUserManager().modifyUser(UUID.fromString(uuid), user -> {
            getLogger().info("Adding user " + uuid + " to LuckPerms group " + group);
            user.data().add(Node.builder(GROUP_NODE_PREFIX + group).build());
        });
    }

    @Override
    public CompletableFuture<Void> removeFromGroup(String group, String uuid) {
        return api.getUserManager().modifyUser(UUID.fromString(uuid), user -> {
            getLogger().info("Removing user " + uuid + " from LuckPerms group " + group);
            user.data().remove(Node.builder(GROUP_NODE_PREFIX + group).build());
        });
    }

    private void onNodeMutate(NodeMutateEvent event) {
        if(!(event.getTarget() instanceof User)) return;

        ConnJson conn = getConnJson();
        if(conn == null || conn.getSyncedRoles().isEmpty()) return;

        User user = (User) event.getTarget();
        String uuidString = user.getUniqueId().toString();

        Set<String> groupsBefore = extractGroupNames(event.getDataBefore());
        Set<String> groupsAfter = extractGroupNames(event.getDataAfter());

        boolean changed = false;

        List<ConnJson.SyncedRole> syncedRolesSnapshot = new ArrayList<>(conn.getSyncedRoles());
        for(ConnJson.SyncedRole role : syncedRolesSnapshot) {
            if(!role.isGroup()) continue;

            String groupName = role.getName();
            boolean wasMember = groupsBefore.contains(groupName);
            boolean isMember = groupsAfter.contains(groupName);
            boolean storedHasMember = role.getPlayers().contains(uuidString);

            if(!wasMember && isMember) {
                if(role.syncsToDiscord()) {
                    getClientManager().addSyncedRoleMember(groupName, true, user.getUniqueId());
                    if(!role.getPlayers().contains(uuidString)) {
                        role.getPlayers().add(uuidString);
                        changed = true;
                    }
                }
                else if(!storedHasMember) {
                    event.getTarget().data().remove(Node.builder(GROUP_NODE_PREFIX + groupName).build());
                }
            }
            else if(wasMember && !isMember) {
                if(role.syncsToDiscord()) {
                    getClientManager().removeSyncedRoleMember(groupName, true, user.getUniqueId());
                    role.getPlayers().remove(uuidString);
                    changed = true;
                }
                else if(storedHasMember) {
                    event.getTarget().data().add(Node.builder(GROUP_NODE_PREFIX + groupName).build());
                }
            }
        }

        if(changed) conn.write();
    }

    private void onGroupDelete(GroupDeleteEvent event) {
        ConnJson conn = getConnJson();
        if(conn == null || conn.getSyncedRoles().isEmpty()) return;

        String groupName = event.getGroupName();
        ConnJson.SyncedRole role = conn.getSyncedRole(groupName, true);
        if(role != null) {
            getLogger().info(MinecraftChatColor.YELLOW + "LuckPerms group '" + groupName + "' was deleted. Removing synced role.");
            getClientManager().removeSyncedRole(groupName, true);
        }
    }

    private Set<String> extractGroupNames(Set<Node> nodes) {
        Set<String> groups = new HashSet<>();
        for(Node node : nodes) {
            String key = node.getKey();
            if(key.startsWith(GROUP_NODE_PREFIX)) groups.add(key.substring(GROUP_NODE_PREFIX.length()));
        }
        return groups;
    }

    @Override
    public List<String> listGroups() {
        return api.getGroupManager().getLoadedGroups()
                .stream()
                .map(Group::getName)
                .collect(Collectors.toList());
    }
}
