package me.lianecx.discordlinker.implementation;

import me.lianecx.discordlinker.abstraction.LinkerPlayer;
import me.lianecx.discordlinker.abstraction.LinkerServer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.util.List;
import java.util.stream.Collectors;

public final class FabricServer implements LinkerServer {

    private final MinecraftServer server;

    public FabricServer(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public String getServerName() {
        return "";
    }

    @Override
    public int getMaxPlayers() {
        return 0;
    }

    @Override
    public int getOnlinePlayersCount() {
        return 0;
    }

    @Override
    public List<LinkerPlayer> getOnlinePlayers() {
        return server.getPlayerList().getPlayers()
                .stream()
                .map(FabricPlayer::new)
                .collect(Collectors.toList());
    }

    @Override
    public LinkerPlayer getPlayer(String username) {
        return null;
    }

    @Override
    public void broadcastMessage(String message) {
        //? if <1.19 {
        /*server.getPlayerList().broadcastMessage(new TextComponent(message), ChatType.CHAT, null);
         *///?} else {
        server.getPlayerList().broadcastSystemMessage(
                Component.literal(message),
                false
        );
        //?}
    }
}