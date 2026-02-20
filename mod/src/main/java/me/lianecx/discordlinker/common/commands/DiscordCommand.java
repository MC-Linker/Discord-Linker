package me.lianecx.discordlinker.common.commands;

import me.lianecx.discordlinker.common.abstraction.LinkerCommandSender;
import me.lianecx.discordlinker.common.abstraction.LinkerPlayer;
import me.lianecx.discordlinker.common.util.MinecraftChatColor;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getClientManager;
import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getConnJson;

public class DiscordCommand implements LinkerMinecraftCommand {

    @Override
    public void execute(LinkerCommandSender sender, String[] args) {
        if(!sender.hasPermission(0, "discordlinker.discord")) {
            sender.sendMessage(MinecraftChatColor.RED + "You do not have permission to use this command!");
            return;
        }
        if(getConnJson() == null) {
            sender.sendMessage(MinecraftChatColor.RED + "There is no connected Discord server. Please use `/connect plugin` in Discord.");
            return;
        }

        getClientManager().getInviteURL(url -> {
            if(url == null) {
                sender.sendMessage(MinecraftChatColor.RED + "An error occurred while getting the invite URL. Please try again later.");
                return;
            }

            if(sender instanceof LinkerPlayer)
                ((LinkerPlayer) sender).sendMessageWithClickableURLs(MinecraftChatColor.GREEN + "Click the following link to join the Discord server: " + MinecraftChatColor.DARK_GREEN + url);
            else
                sender.sendMessage(MinecraftChatColor.GREEN + "Join the Discord server using the following link: " + MinecraftChatColor.DARK_GREEN + url);
        });
    }
}
