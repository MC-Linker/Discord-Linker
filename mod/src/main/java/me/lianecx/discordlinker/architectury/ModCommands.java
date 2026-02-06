package me.lianecx.discordlinker.architectury;

import com.mojang.brigadier.context.CommandContext;
//? if <=1.16.5 {
/*import dev.architectury.event.events.CommandRegistrationEvent;
*///? } else
import dev.architectury.event.events.common.CommandRegistrationEvent;
import me.lianecx.discordlinker.architectury.implementation.ModCommandSender;
import me.lianecx.discordlinker.common.abstraction.LinkerCommandSender;
import net.minecraft.commands.CommandSourceStack;

import java.util.Arrays;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static me.lianecx.discordlinker.architectury.util.SnowflakeCodeArgumentType.snowflakeCode;
import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getMinecraftCommandBus;
import static net.minecraft.commands.Commands.*;

public final class ModCommands {

    public static void registerCommands() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registryAccess/*? if >=1.19 {*/, environment/*? }*/) -> {

            // /linker reload|bot_port|message|private_message|connect|disconnect
            dispatcher.register(literal("linker")
                    .then(literal("connect")
                            .then(argument("code", snowflakeCode())
                                    .executes(ModCommands::forward)
                            )
                    )
                    .then(literal("disconnect").executes(ModCommands::forward))
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
                    .then(argument("code", word())
                            .executes(ModCommands::forward)
                    )
            );

            // /discord
            dispatcher.register(literal("discord")
                    .executes(ModCommands::forward)
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

        LinkerCommandSender sender = new ModCommandSender(context.getSource());

        getMinecraftCommandBus().emitCommand(commandName, sender, args);
        return 1;
    }
}
