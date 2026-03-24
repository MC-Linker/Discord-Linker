package me.lianecx.discordlinker.spigot;

import me.lianecx.discordlinker.common.events.data.AdvancementEventData;
import me.lianecx.discordlinker.spigot.implementation.SpigotPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAchievementAwardedEvent;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getMinecraftEventBus;

public class SpigotAchievementListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAchievement(PlayerAchievementAwardedEvent event) {
        getMinecraftEventBus().emit(new AdvancementEventData(new SpigotPlayer(event.getPlayer()), event.getAchievement().name()));
    }
}
