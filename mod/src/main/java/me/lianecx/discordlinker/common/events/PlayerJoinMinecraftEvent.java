package me.lianecx.discordlinker.common.events;

import me.lianecx.discordlinker.common.abstraction.LinkerPlayer;
import me.lianecx.discordlinker.common.events.data.PlayerJoinEventData;
import me.lianecx.discordlinker.common.util.MinecraftChatColor;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.*;

public class PlayerJoinMinecraftEvent implements LinkerMinecraftEvent<PlayerJoinEventData> {

    public void handle(PlayerJoinEventData event) {
        if(getConnJson() != null && getConnJson().getRequiredRoleToJoin() != null) {
            LinkerPlayer player = event.player;

            getClientManager().hasRequiredRole(player.getUUID(), hasRequiredRoleResponse -> {
                if(hasRequiredRoleResponse == HasRequiredRoleResponse.FALSE)
                    kickPlayerSynchronized(player, MinecraftChatColor.RED + "You do not have the required role(s) to join this server.");
                else if(hasRequiredRoleResponse == HasRequiredRoleResponse.ERROR)
                    kickPlayerSynchronized(player, MinecraftChatColor.RED + "Your roles could not be verified. Please try again later.");
                else if(hasRequiredRoleResponse == HasRequiredRoleResponse.NOT_CONNECTED) {
                    // random 4 digit code
                    int randomCode = (int) (Math.random() * 9000) + 1000;
                    getClientManager().verifyUser(player, randomCode);

                    getClientManager().getInviteURL(url -> {
                        if(url == null) {
                            kickPlayerSynchronized(player, MinecraftChatColor.YELLOW + "You have not connected your Minecraft account to Discord.\nPlease DM " +
                                    MinecraftChatColor.AQUA + "@MC Linker#7784" + MinecraftChatColor.YELLOW + " with the code " +
                                    MinecraftChatColor.AQUA + randomCode + MinecraftChatColor.YELLOW +
                                    " in the next" + MinecraftChatColor.BOLD + " 3 minutes " + MinecraftChatColor.YELLOW + " and rejoin.");
                            return;
                        }
                        kickPlayerSynchronized(player, MinecraftChatColor.YELLOW + "You have not connected your Minecraft account to Discord.\nPlease join " +
                                MinecraftChatColor.AQUA + MinecraftChatColor.UNDERLINE + url + MinecraftChatColor.YELLOW + " and DM " +
                                MinecraftChatColor.AQUA + "@MC Linker#7784" + MinecraftChatColor.YELLOW + " with the code " +
                                MinecraftChatColor.AQUA + randomCode + MinecraftChatColor.YELLOW +
                                " in the next" + MinecraftChatColor.BOLD + " 3 minutes." + MinecraftChatColor.YELLOW + " and rejoin.");
                    });
                }
            });
        }
    }

    public void kickPlayerSynchronized(LinkerPlayer player, String reason) {
        getScheduler().runDelayedSync(() -> getServer().kickPlayer(player, reason), 0);
    }
}
