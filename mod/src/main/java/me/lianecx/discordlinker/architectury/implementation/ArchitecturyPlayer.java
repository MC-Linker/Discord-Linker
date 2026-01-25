package me.lianecx.discordlinker.architectury.implementation;

import me.lianecx.discordlinker.architectury.util.URLComponent;
import me.lianecx.discordlinker.common.abstraction.LinkerPlayer;
//? if <1.19 {
/*import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
 *///? } else
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import static me.lianecx.discordlinker.architectury.util.URLComponent.buildURLComponent;

public class ArchitecturyPlayer extends LinkerPlayer {

    private final ServerPlayer player;

    public ArchitecturyPlayer(ServerPlayer player) {
        super(player.getStringUUID(), player.getName().getString());
        this.player = player;
    }

    @Override
    public void sendMessage(String message) {
        //? if <1.19 {
        /*player.sendMessage(new TextComponent(message), null);
         *///?} else
        player.sendSystemMessage(Component.literal(message));
    }

    @Override
    public void sendMessageWithClickableURLs(String message) {
        Component component = buildURLComponent(message);
        //? if <1.19 {
        /*player.sendMessage(component, null);
        *///? } else
        player.sendSystemMessage(component);
    }

    @Override
    public boolean hasPermission(String permission) {
        // TODO luckperms
        return player.hasPermissions(4); // OP level 4
    }

    @Override
    public void kick(String reason) {
        //? if <1.19 {
        /*player.connection.disconnect(new TextComponent(reason));
         *///? } else
        player.connection.disconnect(Component.literal(reason));
    }

    @Override
    public String getNBTAsString() {
        CompoundTag nbt = new CompoundTag();
        player.save(nbt);
        return nbt.toString();
    }
}
