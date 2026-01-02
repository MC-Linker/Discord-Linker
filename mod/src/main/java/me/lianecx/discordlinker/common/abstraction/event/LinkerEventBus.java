package me.lianecx.discordlinker.common.abstraction.event;

import java.util.*;
import java.util.function.Consumer;

public class LinkerEventBus {

    private final Map<Class<?>, List<Consumer<LinkerEvent>>> listeners = new HashMap<>();

    public <T extends LinkerEvent> void register(Class<T> event, Consumer<LinkerEvent> handler) {
        listeners
                .computeIfAbsent(event, k -> new ArrayList<>())
                .add(handler);
    }

    public void post(LinkerEvent event) {
        List<Consumer<LinkerEvent>> handlers = listeners.get(event.getClass());
        if(handlers == null) return;

        for(Consumer<LinkerEvent> handler : handlers)
            handler.accept(event);
    }

    public void onPlayerJoin(Consumer<PlayerJoinEvent> handler) {
        register(PlayerJoinEvent.class, event -> handler.accept((PlayerJoinEvent) event));
    }

    public void onPlayerQuit(Consumer<PlayerQuitEvent> handler) {
        register(PlayerQuitEvent.class, event -> handler.accept((PlayerQuitEvent) event));
    }

    public void onServerStart(Consumer<ServerStartEvent> handler) {
        register(ServerStartEvent.class, event -> handler.accept((ServerStartEvent) event));
    }

    public void onServerStop(Consumer<ServerStopEvent> handler) {
        register(ServerStopEvent.class, event -> handler.accept((ServerStopEvent) event));
    }
}
