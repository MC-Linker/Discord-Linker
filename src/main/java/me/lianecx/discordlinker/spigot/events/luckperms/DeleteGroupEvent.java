package me.lianecx.discordlinker.spigot.events.luckperms;

import me.lianecx.discordlinker.spigot.DiscordLinker;
import net.luckperms.api.event.group.GroupDeleteEvent;

public class DeleteGroupEvent {

    public static void onGroupDelete(GroupDeleteEvent event) {
        DiscordLinker.getAdapterManager().removeSyncedRole(event.getGroupName(), true);
    }
}
