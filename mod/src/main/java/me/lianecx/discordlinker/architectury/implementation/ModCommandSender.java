package me.lianecx.discordlinker.architectury.implementation;

import me.lianecx.discordlinker.common.abstraction.LinkerCommandSender;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.*;

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
        return source.hasPermission(4);
    }

    @Override
    public String getName() {
        return source.getTextName();
    }
}
