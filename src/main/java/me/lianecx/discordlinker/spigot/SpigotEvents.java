package me.lianecx.discordlinker.spigot;

import me.lianecx.discordlinker.common.events.data.*;
import me.lianecx.discordlinker.spigot.implementation.SpigotPlayer;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.server.ServerCommandEvent;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getMinecraftEventBus;

public class SpigotEvents implements Listener {
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChatMessage(AsyncPlayerChatEvent event) {
        getMinecraftEventBus().emit(new ChatEventData(event.getMessage(), new SpigotPlayer(event.getPlayer())));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        getMinecraftEventBus().emit(new PlayerJoinEventData(new SpigotPlayer(event.getPlayer()), event.getJoinMessage()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        getMinecraftEventBus().emit(new PlayerQuitEventData(new SpigotPlayer(event.getPlayer()), event.getQuitMessage()));
    }

    //? if >=1.12 {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        //Dont process recipes
        if(event.getAdvancement().getKey().toString().startsWith("minecraft:recipes/")) return;
        getMinecraftEventBus().emit(new AdvancementEventData(new SpigotPlayer(event.getPlayer()), event.getAdvancement().getKey().toString()));
    }
    //? } else {
    /*@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAchievement(org.bukkit.event.player.PlayerAchievementAwardedEvent event) {
        getMinecraftEventBus().emit(new AdvancementEventData(new SpigotPlayer(event.getPlayer()), event.getAchievement().name()));
    }
    *///?}

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        getMinecraftEventBus().emit(new PlayerDeathEventData(new SpigotPlayer(event.getEntity()), event.getDeathMessage()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommandPreProcess(PlayerCommandPreprocessEvent event) {
        getMinecraftEventBus().emit(new PlayerCommandEventData(event.getMessage(), new SpigotPlayer(event.getPlayer())));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onServerCommand(ServerCommandEvent event) {
        getMinecraftEventBus().emit(event.getSender() instanceof ConsoleCommandSender ?
                new ConsoleCommandEventData(event.getCommand()) :
                new BlockCommandEventData(event.getCommand())
        );
    }
}
