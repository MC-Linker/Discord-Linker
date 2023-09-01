package me.lianecx.discordlinker.events.luckperms;

import me.lianecx.discordlinker.DiscordLinker;
import me.lianecx.discordlinker.LuckPermsUtil;
import net.luckperms.api.event.node.NodeMutateEvent;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;

import java.util.Set;
import java.util.stream.Collectors;

public class GroupChangeEvent {

    public static void onNodeMutate(NodeMutateEvent event) {
        // Check if the event was acting on a User
        if(!event.isUser()) return;

        // Check if the node was an inheritance node
        Set<InheritanceNode> inheritanceNodes = event.getDataAfter().stream()
                .filter(node -> node.getType() == NodeType.INHERITANCE)
                .map(node -> (InheritanceNode) node)
                .collect(Collectors.toSet());
        if(inheritanceNodes.isEmpty()) return;

        inheritanceNodes.forEach(node -> sendRoleSyncUpdateFromGroup(LuckPermsUtil.LUCK_PERMS.getGroupManager().getGroup(node.getGroupName())));
    }

    private static void sendRoleSyncUpdateFromGroup(Group group) {
        if(group == null) return;
        DiscordLinker.getAdapterManager().sendRoleSyncUpdate(group.getName(), false);
    }
}
