package me.lianecx.discordlinker.spigot.commands;

import me.lianecx.discordlinker.spigot.DiscordLinker;
import me.lianecx.discordlinker.spigot.network.adapters.AdapterManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class LinkerCommand implements CommandExecutor {

    private final DiscordLinker PLUGIN = DiscordLinker.getPlugin();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(args.length == 0) return false;

        switch(args[0]) {
            case "reload":
                PLUGIN.reloadConfig();

                DiscordLinker.getAdapterManager().setHttpPort(PLUGIN.getPort());
                DiscordLinker.getAdapterManager().start(connected -> sender.sendMessage(ChatColor.GREEN + "Successfully reloaded config."));
                break;
            case "bot_port":
                if(args.length == 1) {
                    sender.sendMessage(
                            ChatColor.GREEN + "The current bot_port is " +
                                    ChatColor.DARK_AQUA + PLUGIN.getConfig().getInt("bot_port") +
                                    ChatColor.GREEN + ". The default ist " +
                                    ChatColor.DARK_AQUA + PLUGIN.getConfig().getDefaults().getInt("bot_port") +
                                    ChatColor.GREEN + "."
                    );
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

                if(newPort < 1 || newPort > 65535) {
                    sender.sendMessage(ChatColor.RED + "Please specify a port between 1 and 65535!");
                    return true;
                }

                PLUGIN.getConfig().set("bot_port", newPort);
                PLUGIN.saveConfig();

                AdapterManager.setBotPort(newPort);
                sender.sendMessage(ChatColor.GREEN + "Successfully set bot_port to " + ChatColor.DARK_AQUA + newPort + ChatColor.GREEN + ".");
                break;
            case "connect":
                if(args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Please specify a verification code!");
                    return true;
                }

                if(DiscordLinker.getConnJson() != null) {
                    sender.sendMessage(ChatColor.RED + "The server is already connected! Please disconnect it first using `/linker disconnect`.");
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
