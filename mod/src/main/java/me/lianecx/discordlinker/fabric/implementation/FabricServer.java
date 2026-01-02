package me.lianecx.discordlinker.fabric.implementation;

import me.lianecx.discordlinker.common.abstraction.LinkerPlayer;
import me.lianecx.discordlinker.common.abstraction.LinkerServer;
import net.minecraft.network.chat.ChatType;
//? if <1.19 {
/*import net.minecraft.network.chat.TextComponent;
 *///? } else
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
        return server.getServerModName();
    }

    @Override
    public int getMaxPlayers() {
        return server.getMaxPlayers();
    }

    @Override
    public int getOnlinePlayersCount() {
        return server.getPlayerList().getPlayers().size();
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
         *///?} else
        server.getPlayerList().broadcastSystemMessage(Component.literal(message), false);
    }
}