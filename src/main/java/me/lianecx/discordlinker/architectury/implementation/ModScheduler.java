package me.lianecx.discordlinker.architectury.implementation;

import me.lianecx.discordlinker.common.abstraction.core.LinkerScheduler;
import net.minecraft.server.MinecraftServer;

import java.util.concurrent.*;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getLogger;

public class ModScheduler implements LinkerScheduler {

    /**
     * The number of milliseconds per Minecraft tick. Minecraft runs at 20 ticks per second, so each tick is 50ms.
     * TPS variance (lag) is not accounted for in this scheduler.
     */
    private static final long MS_PER_TICK = 50L;

    private final MinecraftServer server;

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2, r -> {
        Thread t = new Thread(r, "DiscordLinker-Scheduler");
        t.setDaemon(true);
        return t;
    });

    public ModScheduler(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public LinkerSchedulerTask runDelayedSync(Runnable task, int delay) {
        LinkerSchedulerTask wrapper = new LinkerSchedulerTask(task, false, delay);
        ScheduledFuture<?> future = executor.schedule(
                () -> executeOnServer(wrapper),
                delay * MS_PER_TICK, TimeUnit.MILLISECONDS
        );
        ModSchedulerTask modTask = new ModSchedulerTask(wrapper, future);
        getLogger().debug("Scheduled delayed sync task: " + task + " (delay: " + delay + " ticks)");
        return modTask;
    }

    @Override
    public LinkerSchedulerRepeatingTask runRepeatingSync(Runnable task, int delay, int period) {
        LinkerSchedulerRepeatingTask wrapper = new LinkerSchedulerRepeatingTask(task, false, delay, period);
        ScheduledFuture<?> future = executor.scheduleAtFixedRate(
                () -> executeOnServer(wrapper),
                delay * MS_PER_TICK, period * MS_PER_TICK, TimeUnit.MILLISECONDS
        );
        ModSchedulerRepeatingTask modTask = new ModSchedulerRepeatingTask(wrapper, future);
        getLogger().debug("Scheduled repeating sync task: " + task + " (initial delay: " + delay + " ticks, period: " + period + " ticks)");
        return modTask;
    }

    @Override
    public void runSync(Runnable task) {
        getLogger().debug("Scheduled sync task immediately: " + task);
        server.execute(task);
    }

    @Override
    public LinkerSchedulerTask runDelayedAsync(Runnable task, int delay) {
        LinkerSchedulerTask wrapper = new LinkerSchedulerTask(task, true, delay);
        ScheduledFuture<?> future = executor.schedule(
                wrapper::run,
                delay * MS_PER_TICK, TimeUnit.MILLISECONDS
        );
        ModSchedulerTask modTask = new ModSchedulerTask(wrapper, future);
        getLogger().debug("Scheduled delayed async task: " + task + " (delay: " + delay + " ticks)");
        return modTask;
    }

    @Override
    public LinkerSchedulerRepeatingTask runRepeatingAsync(Runnable task, int delay, int period) {
        LinkerSchedulerRepeatingTask wrapper = new LinkerSchedulerRepeatingTask(task, true, delay, period);
        ScheduledFuture<?> future = executor.scheduleAtFixedRate(
                wrapper::run,
                delay * MS_PER_TICK, period * MS_PER_TICK, TimeUnit.MILLISECONDS
        );
        ModSchedulerRepeatingTask modTask = new ModSchedulerRepeatingTask(wrapper, future);
        getLogger().debug("Scheduled repeating async task: " + task + " (initial delay: " + delay + " ticks, period: " + period + " ticks)");
        return modTask;
    }

    @Override
    public void runAsync(Runnable task) {
        executor.execute(task);
        getLogger().debug("Scheduled async task immediately: " + task);
    }

    @Override
    public void shutdown() {
        executor.shutdownNow();
        getLogger().debug("Scheduler shut down.");
    }

    private void executeOnServer(LinkerSchedulerTask wrapper) {
        if(wrapper.isCancelled()) return;
        server.execute(wrapper::run);
    }

    // -------------------------
    // Platform-specific task wrappers
    // -------------------------

    /**
     * Wraps a {@link LinkerSchedulerTask} with a {@link ScheduledFuture} so that
     * cancellation also cancels the pending scheduled execution.
     */
    private static class ModSchedulerTask extends LinkerSchedulerTask {
        private final ScheduledFuture<?> future;

        ModSchedulerTask(LinkerSchedulerTask delegate, ScheduledFuture<?> future) {
            super(delegate.getTask(), delegate.isAsync(), 0);
            this.future = future;
        }

        @Override
        public void cancel() {
            super.cancel();
            future.cancel(false);
        }
    }

    /**
     * Wraps a {@link LinkerSchedulerRepeatingTask} with a {@link ScheduledFuture} so that
     * cancellation also cancels the periodic scheduled execution.
     */
    private static class ModSchedulerRepeatingTask extends LinkerSchedulerRepeatingTask {
        private final ScheduledFuture<?> future;

        ModSchedulerRepeatingTask(LinkerSchedulerRepeatingTask delegate, ScheduledFuture<?> future) {
            super(delegate.getTask(), delegate.isAsync(), 0, delegate.getPeriod());
            this.future = future;
        }

        @Override
        public void cancel() {
            super.cancel();
            future.cancel(false);
        }
    }
}
