package me.lianecx.discordlinker.architectury.implementation;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.lianecx.discordlinker.common.abstraction.LinkerCommandSender;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getTeamsAndGroupsBridge;
//? if >=1.21 {
/*import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
*///? }

public final class ModCommandSender implements LinkerCommandSender {

    private final CommandSourceStack source;

    public ModCommandSender(CommandSourceStack source) {
        this.source = source;
    }

    @Override
    public void sendMessage(String message) {
        //? if <1.19 {
        /*source.sendSuccess(new TextComponent(message), false);
        *///? } else
        source.sendSystemMessage(Component.literal(message));
    }

    @Override
    public boolean hasPermission(int defaultLevel, String permission) {
        ServerPlayer player = null;
        //? if <1.20 {
        /*try {
            player = this.source.getPlayerOrException();
        }
        catch(CommandSyntaxException ignored) {}
        *///? } else
        player = this.source.getPlayer();
        if(player != null && getTeamsAndGroupsBridge().isLuckPermsEnabled())
            return getTeamsAndGroupsBridge().hasPermission(new ModPlayer(player), permission);

        //? if <1.21 {
        return source.hasPermission(defaultLevel);
        //? } else
        //return source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(defaultLevel)));
    }

    @Override
    public String getName() {
        return source.getTextName();
    }
}
