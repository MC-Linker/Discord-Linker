package me.lianecx.discordlinker.common.abstraction.event;

public interface Cancellable {
    boolean isCancelled();

    void setCancelled(boolean cancelled);
}
