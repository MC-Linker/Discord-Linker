package me.lianecx.discordlinker.forge.mixin;

import com.mojang.authlib.GameProfile;
import me.lianecx.discordlinker.common.abstraction.LinkerOfflinePlayer;
import me.lianecx.discordlinker.common.events.JoinRequirementEvaluator;
import me.lianecx.discordlinker.common.events.JoinRequirementMessages;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;

@Mixin(ServerLoginPacketListenerImpl.class)
public abstract class ServerLoginNetHandlerMixin {

    @Shadow GameProfile gameProfile;
    @Shadow public abstract void disconnect(Component reason);

    @Unique private CompletableFuture<JoinRequirementEvaluator.JoinRequirementResult> discordlinker$future;
    @Unique private boolean discordlinker$resolved = false;

    @Inject(method = "handleAcceptedLogin", at = @At("HEAD"), cancellable = true, require = 0)
    private void discordlinker$onHandleAcceptedLogin(CallbackInfo ci) {
        if(discordlinker$resolved) return;

        // Start verification on first call
        if(discordlinker$future == null) discordlinker$future = discordlinker$startVerification();

        // Still waiting for async result - cancel and retry next tick
        if(!discordlinker$future.isDone()) {
            ci.cancel();
            return;
        }

        // Process result once
        discordlinker$resolved = true;
        JoinRequirementEvaluator.JoinRequirementResult result = discordlinker$future.getNow(null);

        if(result != null && result.isAllowed()) return;

        String reason = result != null ? result.getDenyReason() : JoinRequirementMessages.ROLE_CHECK_ERROR;
        disconnect(new TextComponent(reason));
        ci.cancel();
    }

    @Unique
    private CompletableFuture<JoinRequirementEvaluator.JoinRequirementResult> discordlinker$startVerification() {
        GameProfile profile = gameProfile;
        if(profile == null || profile.getName() == null || profile.getName().isEmpty())
            return CompletableFuture.completedFuture(JoinRequirementEvaluator.JoinRequirementResult.deny(JoinRequirementMessages.IDENTITY_CHECK_FAILED));

        String username = profile.getName();
        String uuid = profile.getId() != null ? profile.getId().toString() : LinkerOfflinePlayer.offlineUuid(username);

        CompletableFuture<JoinRequirementEvaluator.JoinRequirementResult> future = new CompletableFuture<>();
        try {
            JoinRequirementEvaluator.evaluate(uuid, username, future::complete);
        }
        catch(Exception e) {
            future.complete(JoinRequirementEvaluator.JoinRequirementResult.deny(JoinRequirementMessages.ROLE_CHECK_ERROR));
        }
        return future;
    }
}
