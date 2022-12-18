package me.lianecx.discordlinker.commands;

import me.lianecx.discordlinker.DiscordLinker;
import me.lianecx.discordlinker.HttpConnection;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VerifyCommand implements CommandExecutor {

    private static final Map<UUID, String> playersAwaitingVerification = new HashMap<>();

    public static void addPlayerToVerificationQueue(UUID uuid, String code) {
        playersAwaitingVerification.put(uuid, code);

        // Remove the player from the queue after 3 minutes
        Bukkit.getScheduler().scheduleSyncDelayedTask(DiscordLinker.getPlugin(), () -> {
            if (playersAwaitingVerification.containsKey(uuid)) {
                playersAwaitingVerification.remove(uuid);
                Bukkit.getPlayer(uuid).sendMessage(ChatColor.YELLOW + "You have been removed from the verification queue because you took too long to verify.");
            }
        }, 20 * 180);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("You must be a player to use this command.");
            return true;
        }
        else if (args.length == 0) return false;

        UUID uuid = ((Player) sender).getUniqueId();
        String code = args[0];
        if (!playersAwaitingVerification.containsKey(uuid)) {
            sender.sendMessage(ChatColor.YELLOW + "You are not awaiting verification. Please execute \"/account connect\" in discord first");
            return true;
        }

        if (!playersAwaitingVerification.get(uuid).equals(code)) {
            sender.sendMessage(ChatColor.RED + "The code you specified is incorrect. Please try again.");
            return true;
        }

        playersAwaitingVerification.remove(uuid);
        HttpConnection.sendVerificationResponse(code, uuid); // Send verification response to the bot
        sender.sendMessage(ChatColor.GREEN + "You have been verified successfully. You can now go back to discord.");

        return true;
    }
}
