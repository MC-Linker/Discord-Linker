package me.lianecx.discordlinker.common.commands;

import me.lianecx.discordlinker.common.abstraction.LinkerCommandSender;

public interface LinkerMinecraftCommand {

    void execute(LinkerCommandSender sender, String[] args);
}
