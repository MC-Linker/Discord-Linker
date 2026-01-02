package me.lianecx.discordlinker.fabric.implementation;

import me.lianecx.discordlinker.common.abstraction.core.LinkerScheduler;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class FabricScheduler implements LinkerScheduler {

    private final Set<LinkerScheduler.LinkerSchedulerTask> tasks = ConcurrentHashMap.newKeySet();

    public FabricScheduler() {
        ServerTickEvents.END_SERVER_TICK.register(this::tick);
    }

    @Override
    public LinkerScheduler.LinkerSchedulerTask runDelayed(Runnable task, int delay) {
        LinkerScheduler.LinkerSchedulerTask wrapper = new LinkerScheduler.LinkerSchedulerTask(task, delay);
        tasks.add(wrapper);
        return wrapper;
    }

    @Override
    public LinkerScheduler.LinkerSchedulerRepeatingTask runRepeating(Runnable task, int initialDelay, int period, int delay) {
        LinkerScheduler.LinkerSchedulerRepeatingTask wrapper =
                new LinkerScheduler.LinkerSchedulerRepeatingTask(task, initialDelay, period);
        tasks.add(wrapper);
        return wrapper;
    }

    private void tick(MinecraftServer server) {
        Iterator<LinkerScheduler.LinkerSchedulerTask> it = tasks.iterator();
        while(it.hasNext()) {
            LinkerScheduler.LinkerSchedulerTask task = it.next();

            if(task.isCancelled()) {
                it.remove();
                continue;
            }

            if(!task.tick()) continue;

            server.execute(task::run);

            if(task instanceof LinkerScheduler.LinkerSchedulerRepeatingTask)
                task.reset(((LinkerScheduler.LinkerSchedulerRepeatingTask) task).getPeriod());
            else it.remove();
        }
    }
}
