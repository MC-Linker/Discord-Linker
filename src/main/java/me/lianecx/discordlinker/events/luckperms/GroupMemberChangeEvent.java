package me.lianecx.discordlinker.events.luckperms;

import me.lianecx.discordlinker.DiscordLinker;
import net.luckperms.api.event.node.NodeMutateEvent;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;

import java.util.Set;
import java.util.stream.Collectors;

public class GroupMemberChangeEvent {

    public static void onNodeMutate(NodeMutateEvent event) {
        if(!event.isUser()) return;
        User user = (User) event.getTarget();

        Set<InheritanceNode> addedInheritance = event.getDataAfter().stream()
                .filter(node -> node.getType() == NodeType.INHERITANCE && !event.getDataBefore().contains(node))
                .map(NodeType.INHERITANCE::cast)
                .collect(Collectors.toSet());

        Set<InheritanceNode> removedInheritance = event.getDataBefore().stream()
                .filter(node -> node.getType() == NodeType.INHERITANCE && !event.getDataAfter().contains(node))
                .map(NodeType.INHERITANCE::cast)
                .collect(Collectors.toSet());
        if(addedInheritance.isEmpty() && removedInheritance.isEmpty()) return;

        addedInheritance.forEach(node -> DiscordLinker.getAdapterManager().addSyncedRoleMember(node.getGroupName(), true, user.getUniqueId()));
        removedInheritance.forEach(node -> DiscordLinker.getAdapterManager().removeSyncedRoleMember(node.getGroupName(), true, user.getUniqueId()));
    }
}
