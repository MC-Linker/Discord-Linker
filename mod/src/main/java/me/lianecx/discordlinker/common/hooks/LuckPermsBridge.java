package me.lianecx.discordlinker.common.hooks;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.matcher.NodeMatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class LuckPermsBridge {

    private final LuckPerms api = LuckPermsProvider.get();

    public CompletableFuture<List<String>> getPlayersInGroup(String group) {
        Group lpGroup = api.getGroupManager().getGroup(group);
        if(lpGroup == null) return CompletableFuture.completedFuture(new ArrayList<>());

        NodeMatcher<Node> matcher = NodeMatcher.key(Node.builder("group." + group).build());
        return api.getUserManager().searchAll(matcher).thenCompose((map) -> {
            List<String> members = map.keySet().stream()
                    .map(UUID::toString)
                    .collect(Collectors.toList());
            return CompletableFuture.completedFuture(members);
        });
    }

    public void addToGroup(String group, String uuid) {
        api.getUserManager().modifyUser(UUID.fromString(uuid), user -> {
            user.data().add(Node.builder("group." + group).build());
        });
    }

    public void removeFromGroup(String group, String uuid) {
        api.getUserManager().modifyUser(UUID.fromString(uuid), user -> {
            user.data().remove(Node.builder("group." + group).build());
        });
    }

    private List<String> getGroupMembers(String group) {
        return api.getUserManager()
                .getLoadedUsers().stream()
                .filter(u -> u.getNodes().contains(Node.builder("group." + group).build()))
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
