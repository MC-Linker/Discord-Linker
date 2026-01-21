package me.lianecx.discordlinker.common.events;

import me.lianecx.discordlinker.common.events.data.MinecraftEventData;

public interface LinkerMinecraftEvent<T extends MinecraftEventData> {

    /**
     * Handle the event with the given data.
     */
    void handle(T data);
}
