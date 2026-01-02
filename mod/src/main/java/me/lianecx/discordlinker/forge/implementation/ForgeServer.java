package me.lianecx.discordlinker.forge.implementation;

import me.lianecx.discordlinker.common.abstraction.LinkerPlayer;
import me.lianecx.discordlinker.common.abstraction.LinkerServer;
//? if <1.19 {
/*import net.minecraft.Util;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.TextComponent;
 *///?} else
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.stream.Collectors;

public class ForgeServer implements LinkerServer {

    private final MinecraftServer server;

    public ForgeServer(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public String getServerName() {
        return null; // Server name is not directly accessible in Forge
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
        return server.getPlayerList().getPlayers().stream()
                .map(ForgePlayer::new)
                .collect(Collectors.toList());
    }

    @Override
    public LinkerPlayer getPlayer(String username) {
        ServerPlayer player = server.getPlayerList().getPlayerByName(username);
        if(player == null) return null;
        return new ForgePlayer(player);
    }

    @Override
    public void broadcastMessage(String message) {
        //? if <1.19 {
        /*server.getPlayerList().broadcastMessage(new TextComponent(message), ChatType.SYSTEM, Util.NIL_UUID);
         *///?} else
        server.getPlayerList().broadcastSystemMessage(Component.literal(message), false);
    }
}
