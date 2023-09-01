package me.lianecx.discordlinker.events;

import me.lianecx.discordlinker.DiscordLinker;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.RemoteServerCommandEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.scoreboard.Team;

import static org.bukkit.Bukkit.getScheduler;
import static org.bukkit.Bukkit.getServer;

public class TeamChangeEvent implements Listener {

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        handleCommand(event.getMessage());
    }

    @EventHandler
    public void onServerCommand(ServerCommandEvent event) {
        handleCommand(event.getCommand());
    }

    @EventHandler
    public void onRemoteCommand(RemoteServerCommandEvent event) {
        handleCommand(event.getCommand());
    }

    private void handleCommand(String command) {
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
                //TODO Parse target selectors

                Team team = getServer().getScoreboardManager().getMainScoreboard().getEntryTeam(args[2]);
                getScheduler().runTaskLater(DiscordLinker.getPlugin(), () -> sendRoleSyncUpdateFromTeam(team), 1);
                break;
            case "remove":
                //TODO remove synced role
                break;
        }
    }

    private void sendRoleSyncUpdateFromTeam(Team team) {
        if(team == null) return;
        DiscordLinker.getAdapterManager().sendRoleSyncUpdate(team.getName(), false);
    }
}
