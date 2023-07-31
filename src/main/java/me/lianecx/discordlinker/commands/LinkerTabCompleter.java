package me.lianecx.discordlinker.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LinkerTabCompleter implements TabCompleter {

    List<String> suggestions = new ArrayList<>();

    public LinkerTabCompleter() {
        suggestions.add("reload");
        suggestions.add("port");
        suggestions.add("message");
        suggestions.add("private_message");
        suggestions.add("connect");
        suggestions.add("disconnect");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if(args.length > 1) return null;
        return suggestions.stream()
                .filter(s -> s.startsWith(args[args.length - 1]))
                .collect(Collectors.toList());
    }
}
