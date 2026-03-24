package me.lianecx.discordlinker.spigot;

import me.lianecx.discordlinker.common.events.data.AdvancementEventData;
import me.lianecx.discordlinker.spigot.implementation.SpigotPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getMinecraftEventBus;

public class SpigotAdvancementListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        //Dont process recipes
        if(event.getAdvancement().getKey().toString().startsWith("minecraft:recipes/")) return;
        getMinecraftEventBus().emit(new AdvancementEventData(new SpigotPlayer(event.getPlayer()), event.getAdvancement().getKey().toString()));
    }
}
