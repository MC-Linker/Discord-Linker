package me.lianecx.discordlinker.architectury.implementation;

import me.lianecx.discordlinker.common.abstraction.LinkerCommandSender;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.*;
//? if >=1.21 {
/*import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.Permissions;
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
    public boolean hasPermission(String permission) {
        //TODO implement this correctly
        //? if <1.21 {
        return source.hasPermission(4);
        //? } else
        //return source.permissions().hasPermission(new Permission.Atom(Identifier.withDefaultNamespace(permission)));
    }

    @Override
    public String getName() {
        return source.getTextName();
    }
}
