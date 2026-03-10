package me.lianecx.discordlinker.architectury;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
//? if <=1.16.5 {
/*import dev.architectury.event.events.CommandRegistrationEvent;
*///? } else
import dev.architectury.event.events.common.CommandRegistrationEvent;
import me.lianecx.discordlinker.architectury.implementation.ModCommandSender;
import me.lianecx.discordlinker.architectury.implementation.ModPlayer;
import me.lianecx.discordlinker.common.ConnJson;
import me.lianecx.discordlinker.common.abstraction.LinkerCommandSender;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
//? if >=1.21 {
/*import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
*///? }

import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getClientManager;
import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getMinecraftCommandBus;
import static net.minecraft.commands.Commands.*;

public final class ModCommands {

    public static void registerCommands() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registryAccess/*? if >=1.19 {*/, environment/*? }*/) -> {

            // /linker reload|bot_port|message|private_message|connect|disconnect
            dispatcher.register(literal("linker")
                    .requires(src -> new ModCommandSender(src).hasPermission(4, "discordlinker.linker"))
                    .then(literal("connect")
                            .then(argument("code", greedyString())
                                    .executes(ModCommands::forward)
                            )
                    )
                    .then(literal("disconnect")
                            .executes(ModCommands::forward))
                    .then(literal("bot_port")
                            .executes(ModCommands::forward)
                            .then(argument("port", integer(1, 65535))
                                    .executes(ModCommands::forward))
                    )
                    .then(literal("debug").executes(ModCommands::forward))
                    .then(literal("reload").executes(ModCommands::forward))
            );

            // /verify <code>
            dispatcher.register(literal("verify")
                    .requires(src -> getClientManager().isConnected())
                    .then(argument("code", word())
                            .executes(ModCommands::forward)
                    )
            );

            // /discord
            dispatcher.register(literal("discord")
                    .requires(src -> getClientManager().isConnected())
                    .executes(ModCommands::forward)
            );

            // /chatload <messages> <duration>
            dispatcher.register(literal("chatload")
                    .requires(src -> new ModCommandSender(src).hasPermission(4, "discordlinker.chatload"))
                    .then(argument("messages", integer(1))
                            .then(argument("duration", integer(1))
                                    .executes(ModCommands::forward)
                                    .then(argument("type", word())
                                            .suggests(ModCommands::suggestChatLoadTypes)
                                            .executes(ModCommands::forward)
                                    )
                            )
                    )
            );
        });
    }

    private static int forward(CommandContext<CommandSourceStack> context) {
        String input = context.getInput();
        String[] split = input.startsWith("/")
                ? input.substring(1).split(" ")
                : input.split(" ");

        String commandName = split[0];
        // Extract arguments (everything after the command name)
        String[] args = split.length > 1
                ? Arrays.copyOfRange(split, 1, split.length)
                : new String[0];


        ServerPlayer player;
        //? if <1.20 {
        /*try {
            player = context.getSource().getPlayerOrException();
        }
        catch(Exception ignored) {}
        *///? } else
        player = context.getSource().getPlayer();

        LinkerCommandSender sender = player != null ?
                new ModPlayer(player) :
                new ModCommandSender(context.getSource());

        getMinecraftCommandBus().emitCommand(commandName, sender, args);
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestChatLoadTypes(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
        for(ConnJson.ChatChannel.ChatChannelType type : ConnJson.ChatChannel.ChatChannelType.values()) {
            String value = type.name().toLowerCase(Locale.ROOT);
            if(value.startsWith(remaining))
                builder.suggest(value);
        }
        return builder.buildFuture();
    }
}
