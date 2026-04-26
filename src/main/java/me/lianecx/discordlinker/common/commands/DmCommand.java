package me.lianecx.discordlinker.common.commands;

import me.lianecx.discordlinker.common.abstraction.LinkerCommandSender;
import me.lianecx.discordlinker.common.abstraction.LinkerPlayer;
import me.lianecx.discordlinker.common.network.protocol.responses.ProtocolError;
import me.lianecx.discordlinker.common.util.MinecraftChatColor;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getClientManager;
import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getConnJson;
import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getScheduler;

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

        getClientManager().sendDm(sender.getName(), user, message, response -> getScheduler().runSync(() -> {
            if(response == null) {
                sender.sendMessage(MinecraftChatColor.RED + "Bot did not respond. Please try again later.");
                return;
            }

            if(response.isSuccess()) {
                sender.sendMessage(MinecraftChatColor.GREEN + "DM sent to " + user + ": " + message);
                return;
            }

            ProtocolError error = response.getError();
            switch(error) {
                case NOT_FOUND:
                    sender.sendMessage(MinecraftChatColor.RED + "Could not find a valid Discord account. You can provide a Discord user ID or username or, only if the user is linked, a Minecraft UUID or username. The user has to be in the linked Discord server.");
                    break;
                case DM_CLOSED:
                    sender.sendMessage(MinecraftChatColor.RED + "That Discord user has DMs disabled.");
                    break;
                case DM_BLOCKED:
                    sender.sendMessage(MinecraftChatColor.RED + "That Discord user has blocked DMs for this request.");
                    break;
                case UNKNOWN:
                default:
                    sender.sendMessage(MinecraftChatColor.RED + "An unknown error occurred while sending the DM. Please try again later.");
                    break;
            }
        }));
    }
}
