package me.lianecx.discordlinker.spigot.implementation;

import me.lianecx.discordlinker.common.abstraction.core.LinkerScheduler;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class SpigotScheduler implements LinkerScheduler {

    private final JavaPlugin plugin;

    public SpigotScheduler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public LinkerSchedulerTask runDelayedSync(Runnable task, int delay) {
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskLater(plugin, task, delay);
        return wrapTask(task, false, delay, bukkitTask);
    }

    @Override
    public LinkerSchedulerRepeatingTask runRepeatingSync(Runnable task, int initialDelay, int period, int delay) {
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, task, initialDelay, period);
        return wrapRepeatingTask(task, false, delay, period, bukkitTask);
    }

    @Override
    public void runAsync(Runnable task) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    @Override
    public LinkerSchedulerTask runDelayedAsync(Runnable task, int delay) {
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delay);
        return wrapTask(task, true, delay, bukkitTask);
    }

    @Override
    public LinkerSchedulerRepeatingTask runRepeatingAsync(Runnable task, int initialDelay, int period, int delay) {
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, initialDelay, period);
        return wrapRepeatingTask(task, true, delay, period, bukkitTask);
    }

    // -------------------------
    // Internal helpers
    // -------------------------

    private LinkerSchedulerTask wrapTask(Runnable runnable, boolean async, int delay, BukkitTask bukkitTask) {
        return new LinkerSchedulerTask(runnable, async, delay) {
            @Override
            public void cancel() {
                super.cancel();
                bukkitTask.cancel();
            }
        };
    }

    private LinkerSchedulerRepeatingTask wrapRepeatingTask(Runnable runnable, boolean async, int delay, int period, BukkitTask bukkitTask) {
        return new LinkerSchedulerRepeatingTask(runnable, async, delay, period) {
            @Override
            public void cancel() {
                super.cancel();
                bukkitTask.cancel();
            }
        };
    }
}
