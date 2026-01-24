package me.lianecx.discordlinker.common.commands;

import me.lianecx.discordlinker.common.abstraction.LinkerCommandSender;
import me.lianecx.discordlinker.common.events.ChatsMinecraftEvent;
import me.lianecx.discordlinker.common.events.LinkerMinecraftEvent;
import me.lianecx.discordlinker.common.events.PlayerJoinMinecraftEvent;
import me.lianecx.discordlinker.common.events.data.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LinkerMinecraftCommandBus {

    private final Map<String, LinkerMinecraftCommand> listeners = new HashMap<>();

    public LinkerMinecraftCommandBus() {
        listeners.put("linker", new LinkerCommand());
        listeners.put("discord", new DiscordCommand());
        listeners.put("verify", new VerifyCommand());
    }

    public void emitCompletion(String commandName, LinkerCommandSender sender, String[] args) {
        if (listeners.containsKey(commandName) && listeners.get(commandName) instanceof LinkerMinecraftCompletableCommand) {
            ((LinkerMinecraftCompletableCommand) listeners.get(commandName)).complete(sender, args);
        }
    }

    public void emitCommand(String commandName, LinkerCommandSender sender, String[] args) {
        if (listeners.containsKey(commandName)) {
            listeners.get(commandName).execute(sender, args);
        }
    }
}
