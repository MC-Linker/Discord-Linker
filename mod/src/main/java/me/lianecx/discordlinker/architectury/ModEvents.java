package me.lianecx.discordlinker.architectury;

import me.lianecx.discordlinker.architectury.implementation.ModPlayer;
import me.lianecx.discordlinker.common.events.data.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
//? if <=1.16.5 {
/*import dev.architectury.event.events.*;
import net.minecraft.world.InteractionResult;
*///? } else {
import dev.architectury.event.events.common.*;
import dev.architectury.event.EventResult;
//? }
import net.minecraft.network.chat.*;

import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getMinecraftEventBus;

public class ModEvents {

    //? if <=1.16.5 {
    /*private static final InteractionResultHolder<Component> PASS_HOLDER = InteractionResultHolder.pass(null);
    *///? } else
    private static final EventResult PASS_HOLDER = EventResult.pass();

    //? if <=1.16.5 {
    /*private static final InteractionResult PASS = InteractionResult.PASS;
    *///? } else
    private static final EventResult PASS = EventResult.pass();

    public static void registerEvents() {
        //? if <1.19 {
        /*ChatEvent.SERVER.register((player, message, component) -> {
            if(player == null || component == null) return PASS_HOLDER;
            String finalMessage = /^? if <=1.16.5 {^/ /^message ^//^? } else {^/ message.getFiltered() /^? }^/;
            getMinecraftEventBus().emit(new ChatEventData(finalMessage, new ModPlayer(player)));
            return PASS_HOLDER;
        });
        *///? } else {
        ChatEvent.DECORATE.register((player, component) -> {
            if(player == null || component == null) return;
            getMinecraftEventBus().emit(new ChatEventData(component.get().getString(), new ModPlayer(player)));
        });
        //?}
        PlayerEvent.PLAYER_ADVANCEMENT.register((player, advancement) -> {
            String id = /*? if >=1.21 {*/ /*advancement.id().toString() *//*? } else {*/ advancement.getId().toString() /*? }*/;
            // Don't process recipes
            if(id.startsWith("minecraft:recipes/")) return;
            getMinecraftEventBus().emit(new AdvancementEventData(new ModPlayer(player), id));
        });
        EntityEvent.LIVING_DEATH.register((entity, source) -> {
            if(!(entity instanceof Player)) return PASS;

            String deathMessage = source.getLocalizedDeathMessage(entity).getString();
            getMinecraftEventBus().emit(new PlayerDeathEventData(new ModPlayer((ServerPlayer) entity), deathMessage));
            return PASS;
        });
        PlayerEvent.PLAYER_JOIN.register(player -> {
            //? if <1.19 {
            /*String joinMessage = new TranslatableComponent("multiplayer.player.joined", player.getDisplayName()).getString();
            *///? } else
            String joinMessage = Component.translatable("multiplayer.player.joined", player.getDisplayName()).getString();
            getMinecraftEventBus().emit(new PlayerJoinEventData(new ModPlayer(player), joinMessage));
        });
        PlayerEvent.PLAYER_QUIT.register(player -> {
            //? if <1.19 {
            /*String quitMessage = new TranslatableComponent("multiplayer.player.left", player.getDisplayName()).getString();
            *///? } else
            String quitMessage = Component.translatable("multiplayer.player.left", player.getDisplayName()).getString();
            getMinecraftEventBus().emit(new PlayerQuitEventData(new ModPlayer(player), quitMessage));
        });
        CommandPerformEvent.EVENT.register((event) -> {
            CommandSourceStack source = event.getResults().getContext().getSource();
            String command = "/" + event.getResults().getReader().getString();

            // Player command
            if(source.getEntity() instanceof ServerPlayer) {
                getMinecraftEventBus().emit(new PlayerCommandEventData(command, new ModPlayer((ServerPlayer) source.getEntity())));
                return PASS;
            }

            // Console command
            if(source.getEntity() == null && "Server".equals(source.getTextName())) {
                getMinecraftEventBus().emit(new ConsoleCommandEventData(command));
                return PASS;
            }

            // Command block command
            if(source.getEntity() == null && "CommandBlock".equals(source.getTextName())) {
                getMinecraftEventBus().emit(new BlockCommandEventData(command));
                return PASS;
            }

            return PASS;
        });
    }
}
