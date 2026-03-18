package me.lianecx.discordlinker.common.commands;

import me.lianecx.discordlinker.common.abstraction.LinkerCommandSender;

import java.util.List;

public interface LinkerMinecraftCompletableCommand extends LinkerMinecraftCommand {

    List<String> complete(LinkerCommandSender sender, String[] args);
}
