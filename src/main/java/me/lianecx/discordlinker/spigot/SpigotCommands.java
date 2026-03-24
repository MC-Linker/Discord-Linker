package me.lianecx.discordlinker.spigot;

import me.lianecx.discordlinker.common.abstraction.LinkerCommandSender;
import me.lianecx.discordlinker.spigot.implementation.SpigotCommandSender;
import me.lianecx.discordlinker.spigot.implementation.SpigotPlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getMinecraftCommandBus;

public class SpigotCommands implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LinkerCommandSender linkerSender = sender instanceof Player ? new SpigotPlayer((Player) sender) : new SpigotCommandSender(sender);
        getMinecraftCommandBus().emitCommand(command.getName(), linkerSender, args);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return getMinecraftCommandBus().emitCompletion(command.getName(), new SpigotCommandSender(sender), args);
    }
}
