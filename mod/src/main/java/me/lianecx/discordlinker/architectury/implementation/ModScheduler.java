package me.lianecx.discordlinker.architectury.implementation;

//? if <1.18 {
/*import dev.architectury.event.events.TickEvent;
 *///? } else

import dev.architectury.event.events.common.TickEvent;
import me.lianecx.discordlinker.common.abstraction.core.LinkerScheduler;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ModScheduler implements LinkerScheduler {

    private static final ExecutorService ASYNC_EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "DiscordLinker-Async");
        t.setDaemon(true);
        return t;
    });

    private final List<LinkerSchedulerTask> tasks = new ArrayList<>();

    public ModScheduler() {
        TickEvent.SERVER_POST.register(this::tick);
    }

    @Override
    public LinkerSchedulerTask runDelayedSync(Runnable task, int delay) {
        LinkerSchedulerTask wrapper = new LinkerSchedulerTask(task, false, delay);
        tasks.add(wrapper);
        return wrapper;
    }

    @Override
    public LinkerSchedulerRepeatingTask runRepeatingSync(Runnable task, int initialDelay, int period, int delay) {
        LinkerSchedulerRepeatingTask wrapper = new LinkerSchedulerRepeatingTask(task, false, initialDelay, period);
        tasks.add(wrapper);
        return wrapper;
    }

    @Override
    public LinkerSchedulerTask runDelayedAsync(Runnable task, int delay) {
        LinkerSchedulerTask wrapper = new LinkerSchedulerTask(task, true, delay);
        tasks.add(wrapper);
        return wrapper;
    }

    @Override
    public LinkerSchedulerRepeatingTask runRepeatingAsync(Runnable task, int initialDelay, int period, int delay) {
        LinkerSchedulerRepeatingTask wrapper = new LinkerSchedulerRepeatingTask(task, true, initialDelay, period);
        tasks.add(wrapper);
        return wrapper;
    }

    /**
     * Ticks all scheduled tasks, runs ready tasks on the server thread
     */
    private void tick(MinecraftServer server) {
        Iterator<LinkerSchedulerTask> it = tasks.iterator();
        while(it.hasNext()) {
            LinkerSchedulerTask task = it.next();

            if(task.isCancelled()) {
                it.remove();
                continue;
            }

            if(!task.tick()) continue;

            if(task.isAsync()) ASYNC_EXECUTOR.execute(task::run);
            else server.execute(task::run);

            if(task instanceof LinkerSchedulerRepeatingTask)
                task.reset(((LinkerSchedulerRepeatingTask) task).getPeriod());
            else it.remove();
        }
    }
}
