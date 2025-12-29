package me.lianecx.discordlinker.common.abstraction;

public interface LinkerEvent {
    boolean isCancelled();

    void setCancelled(boolean cancelled);
}
