package me.lianecx.discordlinker.architectury.implementation;

import me.lianecx.discordlinker.common.abstraction.LinkerCommandSender;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.*;

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
