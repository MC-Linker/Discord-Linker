package me.lianecx.discordlinker.commands;

import me.lianecx.discordlinker.DiscordLinker;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;

public class LinkerCommand implements CommandExecutor {

    DiscordLinker PLUGIN = DiscordLinker.getPlugin();
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(args.length == 0) return false;

        switch (args[0]) {
            case "reload":
                PLUGIN.reloadConfig();

                //Get port from config
                int port = PLUGIN.getConfig().getInt("port") != 0 ? PLUGIN.getConfig().getInt("port") : 11111;
                restartServer(port);
                sender.sendMessage(ChatColor.GREEN + "Successfully reloaded config.");
                break;
            case "port":
                if(args.length == 1) {
                    sender.sendMessage(ChatColor.GREEN + "The current port is " + PLUGIN.getConfig().getInt("port") + ".");
                    return true;
                }

                int newPort;
                try {
                    newPort = Integer.parseInt(args[1]);
                } catch(NumberFormatException | IndexOutOfBoundsException err) {
                    sender.sendMessage(ChatColor.RED + "Please specify a valid port!");
                    return true;
                }

                PLUGIN.getConfig().set("port", newPort);
                PLUGIN.saveConfig();

                restartServer(newPort);
                sender.sendMessage(
                    ChatColor.GREEN + "Successfully set port to " +
                    ChatColor.DARK_AQUA + newPort +
                    ChatColor.GREEN + "."
                );
                break;
            case "private_message":
            case "message":
                if(args.length == 1) {
                    sender.sendMessage(ChatColor.GREEN + "The current " + args[0] + " is " + PLUGIN.getConfig().getString(args[0]) + ".");
                    return true;
                }

                //Join all arguments except first one
                String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

                PLUGIN.getConfig().set(args[0], message);
                PLUGIN.saveConfig();

                sender.sendMessage(
                    ChatColor.GREEN + "Successfully set " + args[0] + " to " +
                    ChatColor.RESET + ChatColor.translateAlternateColorCodes('&', message) +
                    ChatColor.GREEN + "."
                );
                break;
        }

        return true;
    }

    public void restartServer(int port) {
        DiscordLinker.getApp().stop();
        DiscordLinker.getApp().listen(() -> PLUGIN.getLogger().info("Listening on port " + port), port);
    }
}
