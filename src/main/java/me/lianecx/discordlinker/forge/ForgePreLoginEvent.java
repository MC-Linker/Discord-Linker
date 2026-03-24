package me.lianecx.discordlinker.forge;

//? if >1.16.5 {
import com.mojang.authlib.GameProfile;
import me.lianecx.discordlinker.common.abstraction.LinkerOfflinePlayer;
import me.lianecx.discordlinker.common.events.JoinRequirementEvaluator;
import me.lianecx.discordlinker.common.events.JoinRequirementMessages;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
//? if <1.19
//import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
//? if neoforge {
/*import static net.minecraftforge.common.NeoForge.EVENT_BUS;
*///? } else
import static net.minecraftforge.common.MinecraftForge.EVENT_BUS;
import net.minecraftforge.event.entity.player.PlayerNegotiationEvent;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class ForgePreLoginEvent {

    private static volatile boolean registered = false;

    private ForgePreLoginEvent() {}

    public static void register() {
        if(registered) return;
        EVENT_BUS.addListener(ForgePreLoginEvent::onPlayerNegotiation);
        registered = true;
    }

    public static void onPlayerNegotiation(PlayerNegotiationEvent event) {
        GameProfile profile = event.getProfile();
        Connection connection = event.getConnection();

        if(profile == null || connection == null) return;
        //~ if >=1.21 '.getName()' -> '.name()' {
        if(profile.getName() == null || profile.getName().isEmpty()) return;

        String username = profile.getName();
        //~ if >=1.21 'getId()' -> 'id()'
        UUID rawUuid = profile.getId();
        //~ }
        String uuid = (rawUuid != null ? rawUuid : LinkerOfflinePlayer.offlineUuid(username)).toString();

        CompletableFuture<Void> waitFuture = new CompletableFuture<>();
        event.enqueueWork(waitFuture);

        try {
            JoinRequirementEvaluator.evaluate(uuid, username, result -> {
                try {
                    if(!result.isAllowed()) disconnect(connection, result.getDenyReason());
                }
                finally {
                    waitFuture.complete(null);
                }
            });
        }
        catch(Exception e) {
            disconnect(connection, JoinRequirementMessages.ROLE_CHECK_ERROR);
            waitFuture.complete(null);
        }
    }

    private static void disconnect(Connection connection, String reason) {
        //~ if <1.19 'Component.literal' -> 'new TextComponent'
        Component component = Component.literal(reason);
        connection.send(new ClientboundLoginDisconnectPacket(component));
        connection.disconnect(component);
    }
}
//? }
