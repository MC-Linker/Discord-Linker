package me.lianecx.discordlinker.common.abstraction;

import me.lianecx.discordlinker.common.abstraction.event.Cancellable;

public interface LinkerListener {
    void onEvent(Cancellable event);
}
