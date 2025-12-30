package me.lianecx.discordlinker.abstraction;

public interface LinkerEvent {
    boolean isCancelled();

    void setCancelled(boolean cancelled);
}
