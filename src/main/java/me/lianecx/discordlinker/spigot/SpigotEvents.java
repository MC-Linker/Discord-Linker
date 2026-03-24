package me.lianecx.discordlinker.spigot;

import me.lianecx.discordlinker.common.events.JoinRequirementEvaluator;
import me.lianecx.discordlinker.common.events.JoinRequirementMessages;
import me.lianecx.discordlinker.common.events.data.*;
import me.lianecx.discordlinker.spigot.implementation.SpigotPlayer;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.server.ServerCommandEvent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getMinecraftEventBus;

public class SpigotEvents implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        if(event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;

        AtomicReference<JoinRequirementEvaluator.JoinRequirementResult> requirementResult = new AtomicReference<>(null);
        CountDownLatch latch = new CountDownLatch(1);

        String uuid = event.getUniqueId().toString();
        String username = event.getName();

        JoinRequirementEvaluator.evaluate(uuid, username, result -> {
            requirementResult.set(result);
            latch.countDown();
        });

        try {
            if(!latch.await(10, TimeUnit.SECONDS)) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, JoinRequirementMessages.ROLE_CHECK_TIMED_OUT);
                return;
            }
        }
        catch(InterruptedException e) {
            Thread.currentThread().interrupt();
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, JoinRequirementMessages.ROLE_CHECK_INTERRUPTED);
            return;
        }

        JoinRequirementEvaluator.JoinRequirementResult result = requirementResult.get();
        if(result != null && !result.isAllowed()) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, result.getDenyReason());
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChatMessage(AsyncPlayerChatEvent event) {
        getMinecraftEventBus().emit(new ChatEventData(event.getMessage(), new SpigotPlayer(event.getPlayer())));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Requirement checks are handled in AsyncPlayerPreLoginEvent on Spigot.
        getMinecraftEventBus().emit(new PlayerJoinEventData(new SpigotPlayer(event.getPlayer()), event.getJoinMessage(), true));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        getMinecraftEventBus().emit(new PlayerQuitEventData(new SpigotPlayer(event.getPlayer()), event.getQuitMessage()));
    }

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
