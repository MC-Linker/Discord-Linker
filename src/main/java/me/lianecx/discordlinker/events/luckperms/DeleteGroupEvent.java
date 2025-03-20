package me.lianecx.discordlinker.events.luckperms;

import me.lianecx.discordlinker.DiscordLinker;
import net.luckperms.api.event.group.GroupDeleteEvent;

public class DeleteGroupEvent {

    public static void onGroupDelete(GroupDeleteEvent event) {
        DiscordLinker.getWebSocketConnection().removeSyncedRole(event.getGroupName(), true);
    }
}
