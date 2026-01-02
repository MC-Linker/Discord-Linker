package me.lianecx.discordlinker.common.abstraction.core;

public interface LinkerScheduler {

    LinkerSchedulerTask runDelayed(Runnable task, int delay);

    LinkerSchedulerRepeatingTask runRepeating(Runnable task, long initialDelay, long period, int delay);

    class LinkerSchedulerTask {
        private final Runnable task;
        private int ticks;
        private boolean cancelled;

        public LinkerSchedulerTask(Runnable task, int delay) {
            this.task = task;
            this.ticks = delay;
        }

        public Runnable getTask() {
            return task;
        }

        public boolean isCancelled() {
            return cancelled;
        }

        public void cancel() {
            cancelled = true;
        }

        /**
         * Decreases the internal tick counter.
         *
         * @return true if the task is ready to run
         */
        public boolean tick() {
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
            if(!cancelled) task.run();
        }
    }

    class LinkerSchedulerRepeatingTask extends LinkerSchedulerTask {
        private final int period;

        public LinkerSchedulerRepeatingTask(Runnable task, int delay, int period) {
            super(task, delay);
            this.period = period;
        }

        public int getPeriod() {
            return period;
        }
    }
}
