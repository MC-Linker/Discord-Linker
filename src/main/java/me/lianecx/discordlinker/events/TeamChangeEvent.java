package me.lianecx.discordlinker.events;

import me.lianecx.discordlinker.DiscordLinker;
import me.lianecx.discordlinker.utilities.CommandUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.RemoteServerCommandEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.scoreboard.Team;

import java.util.HashSet;
import java.util.Set;

import static org.bukkit.Bukkit.getScheduler;
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

    private void handleCommand(CommandSender sender, String command) {
        if(!command.startsWith("/team") && !command.startsWith("team")) return;
        if(!DiscordLinker.getConnJson().has("synced-roles")) return;
        String[] args = command.split(" ");
        if(args.length < 3) return;

        switch(args[1]) {
            case "join":
            case "empty":
                //Delay to wait for the result of the command and the team to be updated
                getScheduler().runTaskLater(DiscordLinker.getPlugin(), () -> sendRoleSyncUpdateFromTeam(getServer().getScoreboardManager().getMainScoreboard().getTeam(args[2])), 1);
                break;
            case "leave":
                Entity[] selected = CommandUtil.getTargets(sender, args[2]);
                if(selected == null) return;

                Set<Team> teams = new HashSet<>();
                for(Entity entity : selected) {
                    if(!(entity instanceof Player)) continue;
                    Team team = getServer().getScoreboardManager().getMainScoreboard().getEntryTeam(entity.getName());
                    if(team != null) teams.add(team);
                }

                getScheduler().runTaskLater(DiscordLinker.getPlugin(), () -> {
                    for(Team team : teams) sendRoleSyncUpdateFromTeam(team);
                }, 1);
                break;
            case "remove":
                Team team = getServer().getScoreboardManager().getMainScoreboard().getTeam(args[2]);
                if(team != null) DiscordLinker.getAdapterManager().removeSyncedRole(team.getName(), false);
                break;
        }
    }

    private void sendRoleSyncUpdateFromTeam(Team team) {
        if(team == null) return;
        DiscordLinker.getAdapterManager().updateSyncedRole(team.getName(), false);
    }
}
