package me.lianecx.discordlinker.common.commands;

import me.lianecx.discordlinker.common.abstraction.LinkerCommandSender;
import me.lianecx.discordlinker.common.abstraction.LinkerPlayer;
import me.lianecx.discordlinker.common.util.MinecraftChatColor;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getClientManager;
import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getConnJson;

public class DmCommand implements LinkerMinecraftCommand {

    @Override
    public void execute(LinkerCommandSender sender, String[] args) {
        if(!sender.hasPermission(0, "discordlinker.dm")) {
            sender.sendMessage(MinecraftChatColor.RED + "You do not have permission to use this command!");
            return;
        }
        if(getConnJson() == null) {
            sender.sendMessage(MinecraftChatColor.RED + "There is no connected Discord server. Please use `/connect plugin` in Discord.");
            return;
        }
        if(!(sender instanceof LinkerPlayer)) {
            sender.sendMessage(MinecraftChatColor.RED + "Only players can use this command.");
            return;
        }
        if(args.length < 2) {
            sender.sendMessage(MinecraftChatColor.RED + "Usage: /dm <user> <message>");
            return;
        }

        String user = args[0];
        StringBuilder messageBuilder = new StringBuilder();
        for(int i = 1; i < args.length; i++) {
            if(i > 1) messageBuilder.append(' ');
            messageBuilder.append(args[i]);
        }
        String message = messageBuilder.toString();

        getClientManager().sendDm(sender.getName(), user, message, response -> {
            switch(response) {
                case SUCCESS:
                    sender.sendMessage(MinecraftChatColor.GREEN + "DM sent.");
                    break;
                case NOT_CONNECTED:
                    sender.sendMessage(MinecraftChatColor.RED + "Could not find a linked Discord account for that user. You can instead provide a Discord ID or username to specify the recipient.");
                    break;
                case DM_CLOSED:
                    sender.sendMessage(MinecraftChatColor.RED + "That Discord user has DMs disabled.");
                    break;
                case NO_RESPONSE:
                default:
                    sender.sendMessage(MinecraftChatColor.RED + "Bot did not respond. Please try again later.");
                    break;
            }
        });
    }
}
