package me.lianecx.discordlinker.common.abstraction.event;

import me.lianecx.discordlinker.common.abstraction.LinkerPlayer;

public abstract class PlayerJoinEvent implements LinkerEvent, Cancellable {
    private final LinkerPlayer player;

    public PlayerJoinEvent(LinkerPlayer player) {
        this.player = player;
    }

    public LinkerPlayer getPlayer() {
        return player;
    }
}
