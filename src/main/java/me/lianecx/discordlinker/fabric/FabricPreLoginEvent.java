package me.lianecx.discordlinker.fabric;

import me.lianecx.discordlinker.common.abstraction.LinkerOfflinePlayer;
import me.lianecx.discordlinker.common.events.JoinRequirementEvaluator;
import me.lianecx.discordlinker.common.events.JoinRequirementMessages;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
//~ if <1.19 'Component' -> 'TextComponent'
import net.minecraft.network.chat.Component;

import java.util.concurrent.CompletableFuture;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getServer;

public final class FabricPreLoginEvent {

    private static volatile boolean registered = false;

    private FabricPreLoginEvent() {}

    public static void register() {
        if(registered) return;

        ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, synchronizer) -> onQueryStart(handler, server, synchronizer));

        registered = true;
    }

    private static void onQueryStart(ServerLoginPacketListenerImpl handler, MinecraftServer server, ServerLoginNetworking.LoginSynchronizer synchronizer) {
        String handlerUserName = handler.getUserName();
        if(handlerUserName.isEmpty()) {
            disconnect(handler, JoinRequirementMessages.IDENTITY_CHECK_FAILED);
            return;
        }

        String username;
        String uuid;

        //? if <=1.16.5 {
        /*// In older versions, the username is not directly available and must be extracted from the gameprofile string.
        username = extractUsername(handlerUserName);
        uuid = extractUUID(handlerUserName);
        *///? }

        //? if >1.16.5 {
        LinkerOfflinePlayer offlinePlayer = getServer().getOfflinePlayer(handlerUserName);
        uuid = offlinePlayer != null ? offlinePlayer.getUUID() : LinkerOfflinePlayer.offlineUuid(handlerUserName);
        username = handlerUserName;
        //? }

        CompletableFuture<Void> waitFuture = new CompletableFuture<>();
        synchronizer.waitFor(waitFuture);

        try {
            JoinRequirementEvaluator.evaluate(uuid, username, result -> {
                try {
                    if(!result.isAllowed()) disconnect(handler, result.getDenyReason());
                }
                finally {
                    waitFuture.complete(null);
                }
            });
        }
        catch(Exception e) {
            disconnect(handler, JoinRequirementMessages.ROLE_CHECK_ERROR);
            waitFuture.complete(null);
        }
    }

    private static String extractUsername(String gameprofileString) {
        // The game profile string is in the format "com.mojang.authlib.GameProfile@<hash>[id=<username>,name=<uuid>,...]"
        int idIndex = gameprofileString.indexOf("name=");
        if(idIndex == -1) return null;
        int commaIndex = gameprofileString.indexOf(",", idIndex);
        if(commaIndex == -1) return null;
        return gameprofileString.substring(idIndex + 5, commaIndex);
    }

    private static String extractUUID(String gameprofileString) {
        // The game profile string is in the format "com.mojang.authlib.GameProfile@<hash>[id=<username>,name=<uuid>,...]"
        int nameIndex = gameprofileString.indexOf("id=");
        if(nameIndex == -1) return null;
        int commaIndex = gameprofileString.indexOf(",", nameIndex);
        if(commaIndex == -1) return null;
        return gameprofileString.substring(nameIndex + 3, commaIndex);
    }

    private static void disconnect(ServerLoginPacketListenerImpl handler, String reason) {
        //~ if <1.19 'Component.literal' -> 'new TextComponent'
        handler.disconnect(Component.literal(reason));
    }
}
