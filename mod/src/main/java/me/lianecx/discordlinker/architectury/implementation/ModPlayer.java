package me.lianecx.discordlinker.architectury.implementation;

import me.lianecx.discordlinker.common.abstraction.LinkerPlayer;
//? if <1.19
//import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;
//? if >=1.21 {
/*import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueOutput;
*///? }

import static me.lianecx.discordlinker.architectury.util.URLComponent.buildURLComponent;
import static me.lianecx.discordlinker.common.DiscordLinkerCommon.*;

public class ModPlayer extends LinkerPlayer {

    private final ServerPlayer player;

    public ModPlayer(ServerPlayer player) {
        super(player.getStringUUID(), player.getName().getString());
        this.player = player;
    }

    @Override
    public void sendMessage(String message) {
        //? if <1.19 {
        /*player.sendMessage(new TextComponent(message), Util.NIL_UUID);
        *///?} else
        player.sendSystemMessage(Component.literal(message));
    }

    @Override
    public void sendMessageWithClickableURLs(String message) {
        Component component = buildURLComponent(message);
        //? if <1.19 {
        /*player.sendMessage(component, Util.NIL_UUID);
        *///? } else
        player.sendSystemMessage(component);
    }

    @Override
    public boolean hasPermission(int defaultLevel, String permission) {
        if(getTeamsAndGroupsBridge().isLuckPermsEnabled())
            return getTeamsAndGroupsBridge().hasPermission(this, permission);

        //? if <1.21 {
        return player.hasPermissions(defaultLevel);
         //? } else
        //return player.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(defaultLevel)));
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
        //? if <1.21 {
        CompoundTag nbt = new CompoundTag();
        player.saveWithoutId(nbt);
        return nbt.toString();
         //? } else {
        /*TagValueOutput output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        player.saveWithoutId(output);
        return output.buildResult().toString();
        *///? }
    }
}
