package me.lianecx.discordlinker.spigot.commands;

import me.lianecx.discordlinker.spigot.DiscordLinker;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class DiscordCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(DiscordLinker.getConnJson() == null) {
            sender.sendMessage(ChatColor.RED + "There is no connected Discord server. Please use `/connect plugin` in Discord.");
            return true;
        }

        DiscordLinker.getAdapterManager().getInviteURL(url -> {
            if(url == null) {
                sender.sendMessage(ChatColor.RED + "An error occurred while getting the invite URL. Please try again later.");
                return;
            }

            //TODO fix style
            ComponentBuilder builder = new ComponentBuilder("Click here to join the Discord server: " + url)
                    .color(ChatColor.GREEN)
                    .event(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
                    .color(ChatColor.DARK_GREEN);

            sender.spigot().sendMessage(builder.create());
        });
        return true;
    }
}
