package me.lianecx.discordlinker.forge.implementation;

import me.lianecx.discordlinker.common.abstraction.LinkerPlayer;
//? if <1.19 {
/*import net.minecraft.Util;
import net.minecraft.network.chat.TextComponent;
*///?} else
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.awt.*;

public class ForgePlayer implements LinkerPlayer {

    private final ServerPlayer player;

    public ForgePlayer(ServerPlayer player) {
        this.player = player;
    }

    @Override
    public String getName() {
        return player.getName().getString();
    }

    @Override
    public String getUUID() {
        return player.getUUID().toString();
    }

    @Override
    public void sendMessage(String message) {
        //? if <1.19 {
        /*player.sendMessage(new TextComponent(message), Util.NIL_UUID);
         *///?} else
        player.sendSystemMessage(Component.literal(message));
    }

    @Override
    public boolean hasPermission(String permission) {
        // TODO luckperms
        return player.hasPermissions(4);
    }
}
