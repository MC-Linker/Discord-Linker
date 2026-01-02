package me.lianecx.discordlinker.architectury.implementation;

import com.mojang.authlib.GameProfile;
//? if >1.20
//import com.mojang.authlib.yggdrasil.ProfileResult;
import me.lianecx.discordlinker.common.abstraction.LinkerOfflinePlayer;
import me.lianecx.discordlinker.common.abstraction.LinkerPlayer;
import me.lianecx.discordlinker.common.abstraction.LinkerServer;
//? if <1.19 {
/*import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.TextComponent;
 *///? } else
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public final class ArchitecturyServer implements LinkerServer {

    private final MinecraftServer server;

    public ArchitecturyServer(MinecraftServer server) {
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
                .map(ArchitecturyPlayer::new)
                .collect(Collectors.toList());
    }

    @Override
    public LinkerPlayer getPlayer(UUID uuid) {
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if(player == null) return null;
        return new ArchitecturyPlayer(player);
    }

    @Override
    public LinkerOfflinePlayer getOfflinePlayer(String name) {
        // Player is online
        ServerPlayer online = server.getPlayerList().getPlayerByName(name);
        if(online != null) return new ArchitecturyPlayer(online);

        // Player is offline

        //TODO test if this is null in offline mode
        if(server.getProfileCache() == null) return null;

        // may block (creates offline profile if couldn't fetch)
        GameProfile profile = server.getProfileCache()
                .get(name)
                //? if >=1.18
                .orElse(null)
                ;
        if(profile != null) return new LinkerOfflinePlayer(profile.getId().toString(), profile.getName());
        return null;
    }

    @Override
    public LinkerOfflinePlayer getOfflinePlayer(UUID uuid) {
        // Player is online
        LinkerPlayer online = getPlayer(uuid);
        if(online != null) return online;

        // Player is offline

        //TODO test if this is null in offline mode
        if(server.getProfileCache() == null) return null;

        GameProfile profile = server.getProfileCache()
                .get(uuid)
                //? if >=1.18
                .orElse(null)
                ;
        if(profile != null) return new LinkerOfflinePlayer(profile.getId().toString(), profile.getName());

        //? if <1.21 {
        profile = server.getSessionService().fillProfileProperties(new GameProfile(uuid, null), false);
        if(profile == null || profile.getName() == null) return null;
        //? } else {
        /*ProfileResult result = server.getSessionService().fetchProfile(uuid, false);
        if(result != null) profile = result.profile();
        else return null;
        *///? }
        return new LinkerOfflinePlayer(profile.getId().toString(), profile.getName());
    }


    @Override
    public void broadcastMessage(String message) {
        //? if <1.19 {
        /*server.getPlayerList().broadcastMessage(new TextComponent(message), ChatType.CHAT, null);
         *///?} else
        server.getPlayerList().broadcastSystemMessage(Component.literal(message), false);
    }
}