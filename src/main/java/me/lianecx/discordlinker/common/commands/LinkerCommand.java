package me.lianecx.discordlinker.common.commands;

import com.google.common.collect.Lists;
import me.lianecx.discordlinker.common.abstraction.LinkerCommandSender;
import me.lianecx.discordlinker.common.network.client.ClientManager;
import me.lianecx.discordlinker.common.util.MinecraftChatColor;

import java.util.List;
import java.util.stream.Collectors;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.*;

public class LinkerCommand implements LinkerMinecraftCompletableCommand {

    private final List<String> suggestions = Lists.newArrayList("connect", "disconnect", "bot_port", "debug", "reload");

    @Override
    public void execute(LinkerCommandSender sender, String[] args) {
        if(!sender.hasPermission(4, "discordlinker.linker")) {
            sender.sendMessage(MinecraftChatColor.RED + "You do not have permission to use this command!");
            return;
        }
        if(args.length == 0) {
            sender.sendMessage(MinecraftChatColor.RED + "Please specify a subcommand! Usage: /linker <reload|bot_port|connect|disconnect>");
            return;
        }

        switch(args[0]) {
            case "debug":
                boolean newDebug = !getLogger().isDebug();
                getLogger().setDebug(newDebug);
                sender.sendMessage(MinecraftChatColor.GREEN + "Successfully set debug to " + MinecraftChatColor.DARK_AQUA + newDebug + MinecraftChatColor.GREEN + ".");
                break;
            case "reload":
                getConfig().reload();

                getClientManager().reconnect().thenAccept(connected -> {
                    if(connected)
                        sender.sendMessage(MinecraftChatColor.GREEN + "Successfully reloaded the configuration and reconnected to Discord!");
                    else
                        sender.sendMessage(MinecraftChatColor.RED + "Failed to reconnect to Discord!");
                });
                break;
            case "bot_port":
                if(args.length == 1) {
                    sender.sendMessage(
                        MinecraftChatColor.GREEN + "The current bot_port is " +
                        MinecraftChatColor.DARK_AQUA + getConfig().getBotPort() +
                        MinecraftChatColor.GREEN + ". The default is " +
                        MinecraftChatColor.DARK_AQUA + ClientManager.DEFAULT_BOT_PORT +
                        MinecraftChatColor.GREEN + "."
                    );
                    return;
                }

                int newPort;
                try {
                    newPort = Integer.parseInt(args[1]);
                }
                catch(NumberFormatException err) {
                    sender.sendMessage(MinecraftChatColor.RED + "Please specify a valid port number!");
                    return;
                }

                if(newPort < 1 || newPort > 65535) {
                    sender.sendMessage(MinecraftChatColor.RED + "Please specify a port between 1 and 65535!");
                    return;
                }

                getConfig().setBotPort(newPort);

                getClientManager().setBotPort(newPort);
                sender.sendMessage(MinecraftChatColor.GREEN + "Successfully set bot_port to " + MinecraftChatColor.DARK_AQUA + newPort + MinecraftChatColor.GREEN + ".");
                break;
            case "connect":
                if(args.length < 2) {
                    sender.sendMessage(MinecraftChatColor.RED + "Please specify a verification code!");
                    return;
                }

                if(getConnJson() != null) {
                    sender.sendMessage(MinecraftChatColor.RED + "The server is already connected! Please disconnect it first using `/linker disconnect`.");
                    return;
                }

                String code = args[1];
                sender.sendMessage(MinecraftChatColor.YELLOW + "Attempting to connect to the Discord bot...");
                getClientManager().connectWithCode(code).thenAccept(success -> {
                    if(success)
                        sender.sendMessage(MinecraftChatColor.GREEN + "Successfully connected to Discord!");
                    else
                        sender.sendMessage(MinecraftChatColor.RED + "Failed to connect to Discord! Please validate the code and try again.");
                });
                break;
            case "disconnect":
                if(getConnJson() == null) {
                    sender.sendMessage(MinecraftChatColor.RED + "The server is not connected! Please connect it first using `/connect plugin` in Discord.");
                    return;
                }

                getClientManager().disconnectForce();
                getConnJson().delete();
                sender.sendMessage(MinecraftChatColor.GREEN + "Successfully disconnected from Discord!");
                break;
        }
    }

    @Override
    public List<String> complete(LinkerCommandSender sender, String[] args) {
        if(args.length > 1) return null;
        return suggestions.stream()
                .filter(s -> s.startsWith(args[args.length - 1]))
                .collect(Collectors.toList());
    }
}
