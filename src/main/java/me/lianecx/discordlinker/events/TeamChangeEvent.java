package me.lianecx.discordlinker.events;

import me.lianecx.discordlinker.DiscordLinker;
import me.lianecx.discordlinker.utilities.CommandUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.RemoteServerCommandEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.scoreboard.Team;

import static org.bukkit.Bukkit.getServer;

public class TeamChangeEvent implements Listener {

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        handleCommand(event.getPlayer(), event.getMessage());
    }

    @EventHandler
    public void onServerCommand(ServerCommandEvent event) {
        handleCommand(event.getSender(), event.getCommand());
    }

    @EventHandler
    public void onRemoteCommand(RemoteServerCommandEvent event) {
        handleCommand(event.getSender(), event.getCommand());
    }
    //TODO: Add support for other plugins that use teams (possibly timer)

    private void handleCommand(CommandSender sender, String command) {
        if(!command.startsWith("/team") && !command.startsWith("team")) return;
        if(!DiscordLinker.getConnJson().has("synced-roles")) return;
        String[] args = command.split(" ");
        if(args.length < 3) return;

        switch(args[1]) {
            case "join":
                Entity[] joinTargets = CommandUtil.getTargets(sender, args[2]);
                if(joinTargets == null) return;

                for(Entity entity : joinTargets) {
                    if(!(entity instanceof Player)) continue;
                    // Delay to ensure the player is added to the team before the role is added
                    Bukkit.getScheduler().runTaskLater(DiscordLinker.getPlugin(), () -> {
                        Team team = getServer().getScoreboardManager().getMainScoreboard().getEntryTeam(entity.getName());
                        if(team != null)
                            DiscordLinker.getAdapterManager().addSyncedRoleMember(team.getName(), false, entity.getUniqueId());
                    }, 1L);
                }
                break;
            case "empty":
                Team emptiedTeam = getServer().getScoreboardManager().getMainScoreboard().getTeam(args[2]);
                if(emptiedTeam == null) return;

                for(String entry : emptiedTeam.getEntries()) {
                    OfflinePlayer player = Bukkit.getOfflinePlayer(entry);
                    if(player == null) continue;
                    DiscordLinker.getAdapterManager().removeSyncedRoleMember(emptiedTeam.getName(), false, player.getUniqueId());
                }
                break;
            case "leave":
                Entity[] leaveTargets = CommandUtil.getTargets(sender, args[2]);
                if(leaveTargets == null) return;

                for(Entity entity : leaveTargets) {
                    if(!(entity instanceof Player)) continue;

                    Team previousTeam = getServer().getScoreboardManager().getMainScoreboard().getEntryTeam(entity.getName());

                    // Delay to ensure the player is removed from the team before the role is removed
                    Bukkit.getScheduler().runTaskLater(DiscordLinker.getPlugin(), () -> {
                        Team team = getServer().getScoreboardManager().getMainScoreboard().getEntryTeam(entity.getName());
                        if(team == null)
                            DiscordLinker.getAdapterManager().removeSyncedRoleMember(previousTeam.getName(), false, entity.getUniqueId());
                    }, 1L);
                }
                break;
            case "remove":
                Team removedTeam = getServer().getScoreboardManager().getMainScoreboard().getTeam(args[2]);
                if(removedTeam != null)
                    DiscordLinker.getAdapterManager().removeSyncedRole(removedTeam.getName(), false);
                break;
        }
    }
}
