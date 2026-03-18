package me.lianecx.discordlinker.spigot;

import me.lianecx.discordlinker.spigot.implementation.SpigotCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getMinecraftCommandBus;

public class SpigotCommands implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        getMinecraftCommandBus().emitCommand(command.getName(), new SpigotCommandSender(sender), args);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return getMinecraftCommandBus().emitCompletion(command.getName(), new SpigotCommandSender(sender), args);
    }
}
