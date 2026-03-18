package me.lianecx.discordlinker.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
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
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Team;

import java.util.UUID;

import static org.bukkit.Bukkit.getServer;

public class TeamChangeEvent implements Listener {

    private static BukkitTask timer = null;

    public static void startTeamCheck() {
        //Timer checks for team member updates every minute to ensure that changes from other plugins are still synced
        timer = Bukkit.getScheduler().runTaskTimer(DiscordLinker.getPlugin(), () -> {

            if(DiscordLinker.getConnJson() == null || !DiscordLinker.getConnJson().has("synced-roles")) return;
            for(JsonElement syncedRole : DiscordLinker.getConnJson().get("synced-roles").getAsJsonArray()) {
                JsonObject syncedRoleObj = syncedRole.getAsJsonObject();

                Team team = getServer().getScoreboardManager().getMainScoreboard().getTeam(syncedRoleObj.get("name").getAsString());
                if(team == null) continue;

                for(String entry : team.getEntries()) {
                    @SuppressWarnings("deprecation") OfflinePlayer player = Bukkit.getOfflinePlayer(entry);
                    if(syncedRoleObj.get("players").getAsJsonArray().contains(new JsonPrimitive(player.getUniqueId().toString())))
                        continue;
                    DiscordLinker.getAdapterManager().addSyncedRoleMember(team.getName(), false, player.getUniqueId());
                }
                for(JsonElement uuid : syncedRoleObj.get("players").getAsJsonArray()) {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(uuid.getAsString()));
                    if(team.getEntries().contains(offlinePlayer.getName())) continue;
                    DiscordLinker.getAdapterManager().removeSyncedRoleMember(team.getName(), false, offlinePlayer.getUniqueId());
                }
            }
        }, 20 * 60, 20 * 60);
    }

    public static void stopTeamCheck() {
        if(timer != null) timer.cancel();
    }

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
        if(!DiscordLinker.getPlugin().hasTeamSyncedRole()) return;
        String[] args = command.split(" ");

        switch(args[1]) {
            case "join":
                if(args.length < 4) return;

                Entity[] joinTargets = CommandUtil.getTargets(sender, args[3]);
                if(joinTargets == null) return;

                for(Entity entity : joinTargets) {
                    OfflinePlayer player;
                    if(entity == null)
                        //noinspection deprecation
                        player = Bukkit.getOfflinePlayer(args[3]); // Entity is null when the player is offline
                    else if(entity instanceof Player)
                        player = (Player) entity;
                    else continue;

                    // Delay to ensure the player is added to the team before the role is added
                    Bukkit.getScheduler().runTaskLater(DiscordLinker.getPlugin(), () -> {
                        Team team = getServer().getScoreboardManager().getMainScoreboard().getEntryTeam(player.getName());
                        if(team != null)
                            DiscordLinker.getAdapterManager().addSyncedRoleMember(team.getName(), false, player.getUniqueId());
                    }, 1L);
                }
                break;
            case "empty":
                if(args.length < 3) return;

                Team emptiedTeam = getServer().getScoreboardManager().getMainScoreboard().getTeam(args[2]);
                if(emptiedTeam == null) return;

                for(String entry : emptiedTeam.getEntries()) {
                    @SuppressWarnings("deprecation") OfflinePlayer player = Bukkit.getOfflinePlayer(entry);
                    DiscordLinker.getAdapterManager().removeSyncedRoleMember(emptiedTeam.getName(), false, player.getUniqueId());
                }
                break;
            case "leave":
                if(args.length < 4) return;

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
                if(args.length < 3) return;

                Team removedTeam = getServer().getScoreboardManager().getMainScoreboard().getTeam(args[2]);
                if(removedTeam != null)
                    DiscordLinker.getAdapterManager().removeSyncedRole(removedTeam.getName(), false);
                break;
        }
    }
}
