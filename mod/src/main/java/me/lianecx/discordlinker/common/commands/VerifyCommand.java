package me.lianecx.discordlinker.common.commands;

import me.lianecx.discordlinker.common.abstraction.LinkerCommandSender;
import me.lianecx.discordlinker.common.abstraction.LinkerPlayer;
import me.lianecx.discordlinker.common.util.MinecraftChatColor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.*;

public class VerifyCommand implements LinkerMinecraftCommand {

    private static final Map<String, String> playersAwaitingVerification = new HashMap<>();

    public static void addPlayerToVerificationQueue(String uuid, String code) {
        playersAwaitingVerification.put(uuid, code);

        // Remove the player from the queue after 3 minutes
        getScheduler().runDelayedSync(() -> {
            if (playersAwaitingVerification.containsKey(uuid)) {
                playersAwaitingVerification.remove(uuid);

                LinkerPlayer player = getServer().getPlayer(UUID.fromString(uuid));
                if(player != null)
                    player.sendMessage(MinecraftChatColor.YELLOW + "You have been removed from the verification queue because you took too long to verify.");
            }
        }, 20 * 180);
    }

    @Override
    public void execute(LinkerCommandSender sender, String[] args) {
        if(!(sender instanceof LinkerPlayer)) {
            sender.sendMessage("You must be a player to use this command.");
            return;
        }
        else if(!sender.hasPermission("discordlinker.discord")) {
            sender.sendMessage(MinecraftChatColor.RED + "You do not have permission to use this command!");
            return;
        }
        else if(args.length == 0) {
            sender.sendMessage(MinecraftChatColor.RED + "You must specify a verification code. Usage: /verify <code>");
            return;
        }

        String uuid = ((LinkerPlayer) sender).getUUID();
        String code = args[0];
        if(!playersAwaitingVerification.containsKey(uuid)) {
            sender.sendMessage(MinecraftChatColor.YELLOW + "You are not awaiting verification. Please execute \"/account connect\" in discord first");
            return;
        }

        if(!playersAwaitingVerification.get(uuid).equals(code)) {
            sender.sendMessage(MinecraftChatColor.RED + "The code you specified is incorrect. Please try again.");
            return;
        }

        playersAwaitingVerification.remove(uuid);
        getClientManager().sendVerificationResponse(code, uuid); // Send verification response to the bot
        sender.sendMessage(MinecraftChatColor.GREEN + "You have been verified successfully.");
    }
}
