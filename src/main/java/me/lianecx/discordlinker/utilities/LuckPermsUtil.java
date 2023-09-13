package me.lianecx.discordlinker.utilities;

import express.utils.Status;
import me.lianecx.discordlinker.events.luckperms.DeleteGroupEvent;
import me.lianecx.discordlinker.events.luckperms.GroupMemberChangeEvent;
import me.lianecx.discordlinker.network.Router;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.event.group.GroupDeleteEvent;
import net.luckperms.api.event.node.NodeMutateEvent;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.node.matcher.NodeMatcher;
import net.luckperms.api.node.types.InheritanceNode;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class LuckPermsUtil {

    public static final LuckPerms LUCK_PERMS = LuckPermsProvider.get();

    public static void init() {
        LUCK_PERMS.getEventBus().subscribe(NodeMutateEvent.class, GroupMemberChangeEvent::onNodeMutate);
        LUCK_PERMS.getEventBus().subscribe(GroupDeleteEvent.class, DeleteGroupEvent::onGroupDelete);
    }

    public static List<String> getGroupNames() {
        return LUCK_PERMS.getGroupManager().getLoadedGroups().stream().map(Group::getName).collect(Collectors.toList());
    }

    public static Router.RouterResponse updateGroupMembers(String name, List<String> uuids) {
        try {
            Group group = LUCK_PERMS.getGroupManager().getGroup(name);
            if(group == null) {
                return new Router.RouterResponse(Status._404, Router.INVALID_GROUP.toString());
            }

            if(uuids != null) {
                NodeMatcher<InheritanceNode> matcher = NodeMatcher.key(InheritanceNode.builder(group).build());

                LUCK_PERMS.getUserManager().searchAll(matcher).thenAccept((Map<UUID, Collection<InheritanceNode>> map) -> {
                    map.keySet().forEach(uuid -> {
                        if(uuids.contains(uuid.toString())) return;
                        LUCK_PERMS.getUserManager().loadUser(uuid)
                                .thenAcceptAsync(user -> {
                                    if(user == null) return;
                                    user.data().remove(InheritanceNode.builder(group).build());
                                    LUCK_PERMS.getUserManager().saveUser(user);
                                });
                    });

                    uuids.forEach(uuid -> {
                        UUID uuidObj = UUID.fromString(uuid);
                        if(map.containsKey(uuidObj)) return;
                        LUCK_PERMS.getUserManager().loadUser(uuidObj)
                                .thenAcceptAsync(user -> {
                                    if(user == null) return;
                                    user.data().add(InheritanceNode.builder(group).build());
                                    LUCK_PERMS.getUserManager().saveUser(user);
                                });
                    });
                });
            }
            return new Router.RouterResponse(Status._200, Router.SUCCESS.toString());
        }
        catch(Exception err) {
            return new Router.RouterResponse(Status._501, Router.LUCKPERMS_NOT_LOADED.toString());
        }
    }

    public static void getGroupMembers(String name, Consumer<List<String>> callback) {
        Group group = LUCK_PERMS.getGroupManager().getGroup(name);
        if(group == null) {
            callback.accept(new ArrayList<>());
            return;
        }

        List<UUID> players = new ArrayList<>();
        NodeMatcher<InheritanceNode> matcher = NodeMatcher.key(InheritanceNode.builder(group).build());
        LUCK_PERMS.getUserManager().searchAll(matcher).thenAcceptAsync(users -> {
            players.addAll(users.keySet());
            callback.accept(players.stream().map(UUID::toString).collect(Collectors.toList()));
        });
    }
}
