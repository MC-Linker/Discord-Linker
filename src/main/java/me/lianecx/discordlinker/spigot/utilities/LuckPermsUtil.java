package me.lianecx.discordlinker.spigot.utilities;

import express.utils.Status;
import me.lianecx.discordlinker.spigot.DiscordLinker;
import me.lianecx.discordlinker.spigot.events.luckperms.DeleteGroupEvent;
import me.lianecx.discordlinker.spigot.events.luckperms.GroupMemberChangeEvent;
import me.lianecx.discordlinker.spigot.network.Router;
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

    public static void updateGroupMembers(String name, List<String> uuids, boolean onlyAddMembers, Consumer<Router.RouterResponse> callback) {
        try {
            Group group = LUCK_PERMS.getGroupManager().getGroup(name);
            if(group == null) {
                callback.accept(new Router.RouterResponse(Status._404, Router.INVALID_GROUP.toString()));
                return;
            }

            NodeMatcher<InheritanceNode> matcher = NodeMatcher.key(InheritanceNode.builder(group).build());

            LUCK_PERMS.getUserManager().searchAll(matcher).thenAccept((Map<UUID, Collection<InheritanceNode>> map) -> {
                List<String> playersToAdd = new ArrayList<>();

                if(!onlyAddMembers) {
                    map.keySet().forEach(uuid -> {
                        if(uuids.contains(uuid.toString())) return;
                        LUCK_PERMS.getUserManager().loadUser(uuid)
                                .thenAcceptAsync(user -> {
                                    if(user == null) return;
                                    user.data().remove(InheritanceNode.builder(group).build());
                                    LUCK_PERMS.getUserManager().saveUser(user);
                                });
                    });
                }

                uuids.forEach(uuid -> {
                    UUID uuidObj = UUID.fromString(uuid);
                    if(map.containsKey(uuidObj)) return;
                    playersToAdd.add(uuid);
                });

                Set<String> allPlayers = new HashSet<>(playersToAdd);

                // If we're only adding members, previous players will be kept in the group so we have to fetch all group members
                if(onlyAddMembers) getGroupMembers(name, players -> {
                    allPlayers.addAll(players);
                    callback.accept(new Router.RouterResponse(Status._200, DiscordLinker.getGson().toJson(allPlayers)));
                });
                    // Otherwise the members will match the uuids list
                else callback.accept(new Router.RouterResponse(Status._200, DiscordLinker.getGson().toJson(uuids)));

                playersToAdd.forEach(uuid -> LUCK_PERMS.getUserManager().loadUser(UUID.fromString(uuid))
                        .thenAcceptAsync(user -> {
                            if(user == null) return;
                            user.data().add(InheritanceNode.builder(group).build());
                            LUCK_PERMS.getUserManager().saveUser(user);
                        }));
            });
        }
        catch(Exception err) {
            callback.accept(new Router.RouterResponse(Status._501, Router.LUCKPERMS_NOT_LOADED.toString()));
        }
    }

    public static void getGroupMembers(String name, Consumer<List<String>> callback) {
        Group group = LUCK_PERMS.getGroupManager().getGroup(name);
        if(group == null) {
            callback.accept(null);
            return;
        }

        List<UUID> players = new ArrayList<>();
        NodeMatcher<InheritanceNode> matcher = NodeMatcher.key(InheritanceNode.builder(group).build());
        LUCK_PERMS.getUserManager().searchAll(matcher).thenAcceptAsync(users -> {
            players.addAll(users.keySet());
            callback.accept(players.stream().map(UUID::toString).collect(Collectors.toList()));
        });
    }

    public static void removeGroupFromUser(UUID uuid, String name) {
        LUCK_PERMS.getUserManager().loadUser(uuid).thenAcceptAsync(user -> {
            if(user == null) return;
            user.data().remove(InheritanceNode.builder(name).build());
            LUCK_PERMS.getUserManager().saveUser(user);
        });
    }

    public static void addGroupToUser(UUID uuid, String name) {
        LUCK_PERMS.getUserManager().loadUser(uuid).thenAcceptAsync(user -> {
            if(user == null) return;
            user.data().add(InheritanceNode.builder(name).build());
            LUCK_PERMS.getUserManager().saveUser(user);
        });
    }

    public static void updateUserGroup(String name, UUID uuid, String addOrRemove, Consumer<Router.RouterResponse> callback) {
        Group group = LUCK_PERMS.getGroupManager().getGroup(name);
        if(group == null) {
            callback.accept(new Router.RouterResponse(Status._404, Router.INVALID_GROUP.toString()));
            return;
        }

        LUCK_PERMS.getUserManager().loadUser(uuid).thenAcceptAsync(user -> {
            if(user == null) {
                callback.accept(new Router.RouterResponse(Status._404, Router.INVALID_PLAYER.toString()));
                return;
            }

            if(addOrRemove.equals("add")) user.data().add(InheritanceNode.builder(group).build());
            else if(addOrRemove.equals("remove")) user.data().remove(InheritanceNode.builder(group).build());
            LUCK_PERMS.getUserManager().saveUser(user);
            callback.accept(new Router.RouterResponse(Status._200, Router.SUCCESS.toString()));
        });
    }
}
