package me.lianecx.discordlinker.events;

import me.lianecx.discordlinker.DiscordLinker;
import me.lianecx.discordlinker.network.HasRequiredRoleResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if(DiscordLinker.getConnJson() == null) return;
        if(DiscordLinker.getConnJson().has("requiredRoleToJoin")) {
            Player player = event.getPlayer();

            DiscordLinker.getAdapterManager().hasRequiredRole(event.getPlayer().getUniqueId(), hasRequiredRoleResponse -> {
                if(hasRequiredRoleResponse == HasRequiredRoleResponse.FALSE)
                    kickPlayerSynchronized(player, ChatColor.RED + "You do not have the required role to join this server.");
                else if(hasRequiredRoleResponse == HasRequiredRoleResponse.NOT_CONNECTED) {
                    // random 4 digit code
                    int randomCode = (int) (Math.random() * 9000) + 1000;
                    DiscordLinker.getAdapterManager().verifyUser(event.getPlayer(), randomCode);

                    DiscordLinker.getAdapterManager().getInviteURL(url -> {
                        if(url == null) {
                            kickPlayerSynchronized(player, ChatColor.YELLOW + "You have not connected your Minecraft account to Discord.\nPlease DM " +
                                    ChatColor.AQUA + "@MC Linker#7784" + ChatColor.YELLOW + " with the code " +
                                    ChatColor.AQUA + randomCode + ChatColor.YELLOW +
                                    " in the next" + ChatColor.BOLD + " 3 minutes " + ChatColor.YELLOW + " and rejoin.");
                            return;
                        }
                        kickPlayerSynchronized(player, ChatColor.YELLOW + "You have not connected your Minecraft account to Discord.\nPlease join " +
                                ChatColor.AQUA + url + ChatColor.YELLOW + " and DM " +
                                ChatColor.AQUA + "@MC Linker#7784" + ChatColor.YELLOW + " with the code " +
                                ChatColor.AQUA + randomCode + ChatColor.YELLOW +
                                " in the next" + ChatColor.BOLD + " 3 minutes.");
                    });
                }
                else if(hasRequiredRoleResponse == HasRequiredRoleResponse.ERROR)
                    kickPlayerSynchronized(player, ChatColor.RED + "Your roles could not be verified. Please try again later.");
            });
        }
    }

    public void kickPlayerSynchronized(Player player, String reason) {
        Bukkit.getScheduler().runTask(DiscordLinker.getPlugin(), () -> player.kickPlayer(reason));
    }
}
