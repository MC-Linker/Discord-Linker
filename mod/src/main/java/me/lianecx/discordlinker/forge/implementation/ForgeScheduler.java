package me.lianecx.discordlinker.forge.implementation;

import me.lianecx.discordlinker.common.abstraction.core.LinkerScheduler;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
//? if <1.19
//import net.minecraftforge.fml.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeScheduler implements LinkerScheduler {

    private final List<LinkerSchedulerTask> tasks = new ArrayList<>();

    public ForgeScheduler() {
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if(event.phase != TickEvent.Phase.END) return;
        if(event.side != LogicalSide.SERVER) return;

        //? if <1.19 {
        /*MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
         *///?} else
        MinecraftServer server = event.getServer();
        if(server == null) return;

        tick(server);
    }

    @Override
    public LinkerSchedulerTask runDelayed(Runnable task, int delay) {
        LinkerSchedulerTask wrapper = new LinkerSchedulerTask(task, delay);
        tasks.add(wrapper);
        return wrapper;
    }

    @Override
    public LinkerSchedulerRepeatingTask runRepeating(Runnable task, int initialDelay, int period, int delay) {
        LinkerSchedulerRepeatingTask wrapper = new LinkerSchedulerRepeatingTask(task, initialDelay, period);
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

            server.execute(task::run);

            if(task instanceof LinkerSchedulerRepeatingTask)
                task.reset(((LinkerSchedulerRepeatingTask) task).getPeriod());
            else it.remove();
        }
    }
}
