package me.lianecx.discordlinker.common.abstraction.core;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getLogger;

public interface LinkerScheduler {

    LinkerSchedulerTask runDelayedSync(Runnable task, int delay);

    LinkerSchedulerRepeatingTask runRepeatingSync(Runnable task, int delay, int period);
    
    void runSync(Runnable task);

    LinkerSchedulerTask runDelayedAsync(Runnable task, int delay);

    LinkerSchedulerRepeatingTask runRepeatingAsync(Runnable task, int delay, int period);

    void runAsync(Runnable task);

    /**
     * Shuts down the scheduler and releases resources.
     * Called when the server is stopping.
     */
    default void shutdown() {}

    class LinkerSchedulerTask {
        private final Runnable task;
        private final boolean async;
        private int ticks;
        private volatile boolean cancelled;

        public LinkerSchedulerTask(Runnable task, boolean async, int delay) {
            this.task = task;
            this.async = async;
            this.ticks = delay;
        }

        public Runnable getTask() {
            return task;
        }

        public boolean isAsync() {
            return async;
        }

        public boolean isCancelled() {
            return cancelled;
        }

        public void cancel() {
            getLogger().debug("Cancelling task: " + task + " (async: " + async + ")");
            cancelled = true;
        }

        /**
         * Decreases the internal tick counter.
         *
         * @return true if the task is ready to run
         */
        public boolean tick() {
//            getLogger().debug("Ticking task: " + task + " (ticks remaining: " + ticks + ")");
            if(cancelled) return false;
            return --ticks <= 0;
        }

        /**
         * Resets the internal tick counter.
         *
         * @param delay the delay to reset to
         */
        public void reset(int delay) {
            this.ticks = delay;
        }

        public void run() {
            getLogger().debug("Running task: " + task + " (async: " + async + ")");
            if(!cancelled) task.run();
        }
    }

    class LinkerSchedulerRepeatingTask extends LinkerSchedulerTask {
        private final int period;

        public LinkerSchedulerRepeatingTask(Runnable task, boolean async, int delay, int period) {
            super(task, async, delay);
            this.period = period;
        }

        public int getPeriod() {
            return period;
        }
    }
}
