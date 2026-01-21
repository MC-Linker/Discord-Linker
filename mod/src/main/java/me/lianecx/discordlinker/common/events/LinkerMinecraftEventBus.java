package me.lianecx.discordlinker.common.events;

import me.lianecx.discordlinker.common.events.data.*;
import net.minecraft.client.multiplayer.chat.ChatListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LinkerMinecraftEventBus {

    private final Map<Class<? extends MinecraftEventData>, List<LinkerMinecraftEvent<? extends MinecraftEventData>>> listeners = new HashMap<>();

    public LinkerMinecraftEventBus() {
        register(ServerStartEventData.class, ChatsMinecraftEvent::handleServerStart);
        register(ServerStopEventData.class, ChatsMinecraftEvent::handleServerStop);
        register(ChatEventData.class, ChatsMinecraftEvent::handleChat);
        register(PlayerJoinEventData.class, ChatsMinecraftEvent::handleJoin);
        register(PlayerQuitEventData.class, ChatsMinecraftEvent::handleQuit);
        register(AdvancementEventData.class, ChatsMinecraftEvent::handleAdvancement);
        register(PlayerDeathEventData.class, ChatsMinecraftEvent::handlePlayerDeath);
        register(PlayerCommandEventData.class, ChatsMinecraftEvent::handlePlayerCommand);
        register(ConsoleCommandEventData.class, ChatsMinecraftEvent::handleConsoleCommand);
        register(BlockCommandEventData.class, ChatsMinecraftEvent::handleBlockCommand);

        register(PlayerJoinEventData.class, new PlayerJoinMinecraftEvent());
    }

    private <T extends MinecraftEventData> void register(Class<T> type, LinkerMinecraftEvent<T> handler) {
        listeners
                .computeIfAbsent(type, k -> new ArrayList<>())
                .add(handler);
    }

    public <T extends MinecraftEventData> void emit(T data) {
        List<LinkerMinecraftEvent<? extends MinecraftEventData>> eventListeners = listeners.get(data.getClass());
        if(eventListeners == null) return;

        for (LinkerMinecraftEvent<? extends MinecraftEventData> listener : eventListeners) {
            @SuppressWarnings("unchecked")
            LinkerMinecraftEvent<T> event = (LinkerMinecraftEvent<T>) listener;
            event.handle(data);
        }
    }
}
