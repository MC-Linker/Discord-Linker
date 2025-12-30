package me.lianecx.discordlinker.fabric.implementation;

import me.lianecx.discordlinker.common.abstraction.LinkerPlayer;
import net.minecraft.world.entity.player.Player;


public final class FabricPlayer implements LinkerPlayer {

    private final Player player;

    public FabricPlayer(Player player) {
        this.player = player;
    }

    @Override
    public String getUUID() {
        return player.getUUID().toString();
    }

    @Override
    public String getName() {
        return player.getName().getString();
    }

    @Override
    public void sendMessage(String message) {
        //? if <1.19 {
        /*player.sendMessage(new TextComponent(message), null);
         *///?} else
        player.sendSystemMessage(Component.literal(message));
    }

    @Override
    public boolean hasPermission(String permission) {
        // TODO luckperms
        return player.hasPermissions(4); // OP level 4
    }
}