package me.lianecx.discordlinker.commands;

import me.lianecx.discordlinker.DiscordLinker;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;

public class LinkerCommand implements CommandExecutor {

    private final DiscordLinker PLUGIN = DiscordLinker.getPlugin();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(args.length == 0) return false;

        switch(args[0]) {
            case "reload":
                PLUGIN.reloadConfig();

                DiscordLinker.getAdapterManager().setHttpPort(PLUGIN.getPort());
                DiscordLinker.getAdapterManager().startAll(connected -> sender.sendMessage(ChatColor.GREEN + "Successfully reloaded config."));
                break;
            case "port":
                if(args.length == 1) {
                    sender.sendMessage(ChatColor.GREEN + "The current port is " + PLUGIN.getConfig().getInt("port") + ".");
                    return true;
                }

                int newPort;
                try {
                    newPort = Integer.parseInt(args[1]);
                }
                catch(NumberFormatException | IndexOutOfBoundsException err) {
                    sender.sendMessage(ChatColor.RED + "Please specify a valid port!");
                    return true;
                }

                PLUGIN.getConfig().set("port", newPort);
                PLUGIN.saveConfig();

                DiscordLinker.getAdapterManager().setHttpPort(newPort);
                //Only start http server if it was already started
                if(DiscordLinker.getAdapterManager().isHttpConnected()) DiscordLinker.getAdapterManager().startHttp();
                sender.sendMessage(ChatColor.GREEN + "Successfully set port to " + ChatColor.DARK_AQUA + newPort + ChatColor.GREEN + ".");
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

                sender.sendMessage(ChatColor.GREEN + "Successfully set " + args[0] + " to " + ChatColor.RESET + ChatColor.translateAlternateColorCodes('&', message) + ChatColor.GREEN + ".");
                break;
            case "connect":
                if(args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Please specify a verification code!");
                    return true;
                }

                if(DiscordLinker.getConnJson() != null) {
                    sender.sendMessage(ChatColor.RED + "The server is already connected! Please disconnect it first using `/disconnect` in Discord.");
                    return true;
                }

                String code = args[1];
                sender.sendMessage(ChatColor.YELLOW + "Attempting to connect to the Discord bot...");
                DiscordLinker.getAdapterManager().connectWebsocket(code, success -> {
                    if(success)
                        sender.sendMessage(ChatColor.GREEN + "Successfully connected to Discord!");
                    else
                        sender.sendMessage(ChatColor.RED + "Failed to connect to Discord! Please validate the code and try again.");
                });
                break;
            //Add disconnect
            case "disconnect":
                if(DiscordLinker.getConnJson() == null) {
                    sender.sendMessage(ChatColor.RED + "The server is not connected! Please connect it first using `/connect plugin` in Discord.");
                    return true;
                }

                DiscordLinker.getAdapterManager().disconnectForce();
                sender.sendMessage(ChatColor.GREEN + "Successfully disconnected from Discord!");
                break;
            default:
                return false;
        }

        return true;
    }
}
