package me.lianecx.discordlinker.abstraction.core;

public interface LinkerScheduler {

    LinkerSchedulerTask runDelayed(Runnable task, int delay);

    LinkerSchedulerRepeatingTask runRepeating(Runnable task, long initialDelay, long period, int delay);

    void cancel(Runnable task);

    class LinkerSchedulerTask {
        private final Runnable task;
        private int ticks;
        private boolean cancelled;

        public LinkerSchedulerTask(Runnable task, int delay) {
            this.task = task;
            this.ticks = delay;
        }

        public Runnable getTask() {return task;}

        public boolean isCancelled() {return cancelled;}

        public void cancel() {cancelled = true;}

        public boolean tick() {
            if(cancelled) return false;
            return --ticks <= 0;
        }

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

        public int getPeriod() {return period;}
    }
}
