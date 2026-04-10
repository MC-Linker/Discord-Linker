package me.lianecx.discordlinker.common.commands;

import me.lianecx.discordlinker.common.abstraction.LinkerCommandSender;
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
        listeners.put("dm", new DmCommand());
    }

    /**
     * Emits a command tab completion event.
     * Keep in mind that mod's directly set arguments through brigadier and won't emit this event.
     */
    public List<String> emitCompletion(String commandName, LinkerCommandSender sender, String[] args) {
        if (listeners.containsKey(commandName) && listeners.get(commandName) instanceof LinkerMinecraftCompletableCommand) {
            return ((LinkerMinecraftCompletableCommand) listeners.get(commandName)).complete(sender, args);
        }
        return new ArrayList<>();
    }

    public void emitCommand(String commandName, LinkerCommandSender sender, String[] args) {
        if (listeners.containsKey(commandName)) {
            listeners.get(commandName).execute(sender, args);
        }
    }
}
