package me.lianecx.discordlinker.architectury.implementation;

import com.mojang.authlib.GameProfile;
//? if >=1.21
//import com.mojang.authlib.yggdrasil.ProfileResult;
import dev.architectury.platform.Platform;
import me.lianecx.discordlinker.common.abstraction.LinkerOfflinePlayer;
import me.lianecx.discordlinker.common.abstraction.LinkerPlayer;
import me.lianecx.discordlinker.common.abstraction.LinkerServer;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static me.lianecx.discordlinker.architectury.util.URLComponent.buildURLComponent;

public final class ModServer implements LinkerServer {

    private final MinecraftServer server;

    public ModServer(MinecraftServer server) {
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
                .map(ModPlayer::new)
                .collect(Collectors.toList());
    }

    @Override
    public String getDataFolder() {
        return Platform.getConfigFolder().resolve("discord-linker").toString();
    }

    @Override
    public LinkerPlayer getPlayer(UUID uuid) {
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if(player == null) return null;
        return new ModPlayer(player);
    }

    @Override
    public LinkerOfflinePlayer getOfflinePlayer(String name) {
        // Player is online
        ServerPlayer online = server.getPlayerList().getPlayerByName(name);
        if(online != null) return new ModPlayer(online);

        // Player is offline

        //TODO test if this is null in offline mode
        if(server.getProfileCache() == null) return null;

        // may block (creates offline profile if couldn't fetch)
        GameProfile profile = server.getProfileCache()
                //? if <1.18 {
                /*.get(name);
                *///? } else {
                .get(name)
                .orElse(null);//? }
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
                //? if <1.18 {
                /*.get(uuid);
                *///? } else {
                .get(uuid)
                .orElse(null);//? }

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

    @Override
    public void broadcastMessageWithClickableURLs(String message) {
        Component component = buildURLComponent(message);
        //? if <1.19 {
        /*server.getPlayerList().broadcastMessage(component, ChatType.CHAT, null);
        *///?} else
        server.getPlayerList().broadcastSystemMessage(component, false);
    }

    @Override
    public boolean isPluginOrModEnabled(String pluginNameOrModId) {
        return Platform.isModLoaded(pluginNameOrModId);
    }

    @Override
    public boolean isOnline() {
        return server.getPreventProxyConnections();
    }

    @Override
    public String getMinecraftVersion() {
        return Platform.getMinecraftVersion();
    }

    @Override
    public String getWorldPath() {
        return server.getWorldPath(LevelResource.ROOT).toAbsolutePath().toString();
    }

    @Override
    public String getWorldContainerPath() {
        return server.getWorldPath(LevelResource.ROOT).getParent().toAbsolutePath().toString();
    }

    @Override
    public @Nullable String getFloodgatePrefix() {
        return null;
    }

    @Override
    public CompletableFuture<String> executeCommand(String command) {
        CompletableFuture<String> future = new CompletableFuture<>();
        StringBuilder output = new StringBuilder();

        ServerLevel serverLevel = server.overworld();
        CommandSourceStack source = new CommandSourceStack(
                new CommandSource() {

                    //? if >=1.19 {
                    @Override
                    public void sendSystemMessage(Component component) {
                        output.append(component.getString()).append('\n');
                    }

                    //? } else {
                    /*@Override
                    public void sendMessage(@NotNull Component component, @NotNull UUID senderUUID) {
                        output.append(component.getString()).append('\n');
                    }
                    *///? }

                    @Override
                    public boolean acceptsSuccess() {
                        return true;
                    }

                    @Override
                    public boolean acceptsFailure() {
                        return true;
                    }

                    @Override
                    public boolean shouldInformAdmins() {
                        return false;
                    }
                },
                serverLevel == null ? Vec3.ZERO : Vec3.atLowerCornerOf(serverLevel.getSharedSpawnPos()),
                Vec2.ZERO,
                server.overworld(),
                4, // permission level
                "Discord",
                //? if <1.19 {
                /*new TextComponent("Discord"),
                 *///? } else
                Component.literal("Discord"),
                server,
                null
        ).withCallback((context, success/*? if <1.21 {*/, result /*? }*/) -> {
            String outStr = output.toString().trim();
            if(outStr.isEmpty())
                outStr = success /*? if >=1.21 {*/ /*> 0 *//*? }*/ ? COMMAND_NO_OUTPUT_SUCCESS : COMMAND_NO_OUTPUT_FAIL;
            future.complete(outStr);
        });

        try {
            //? if >=1.19 {
            server.getCommands().performPrefixedCommand(source, command);
             //? } else
            //server.getCommands().performCommand(source, command);
        }
        catch(Exception e) {
            future.complete("Error executing command: " + e.getMessage());
        }

        return future;
    }
}