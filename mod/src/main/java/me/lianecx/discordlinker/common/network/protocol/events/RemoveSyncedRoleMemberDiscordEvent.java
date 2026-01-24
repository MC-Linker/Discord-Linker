package me.lianecx.discordlinker.common.network.protocol.events;

import me.lianecx.discordlinker.common.ConnJson;
import me.lianecx.discordlinker.common.network.protocol.payloads.InvalidPayloadException;
import me.lianecx.discordlinker.common.network.protocol.payloads.SyncedRoleMemberPayload;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventJsonResponse;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventResponse;

import java.util.concurrent.CompletableFuture;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.*;

public class RemoveSyncedRoleMemberDiscordEvent implements LinkerDiscordEvent<SyncedRoleMemberPayload> {

    @Override
    public SyncedRoleMemberPayload decode(Object[] objects) throws InvalidPayloadException {
        return SyncedRoleMemberPayload.decode(objects);
    }

    @Override
    public CompletableFuture<DiscordEventResponse> handleAsync(SyncedRoleMemberPayload payload) {
        if(getConnJson() == null) return CompletableFuture.completedFuture(DiscordEventJsonResponse.CONN_JSON_MISSING);

        if(payload.role.isGroup() && !getServer().isPluginOrModEnabled("LuckPerms")){
            return CompletableFuture.completedFuture(DiscordEventJsonResponse.LUCKPERMS_NOT_LOADED);
        }

        CompletableFuture<DiscordEventResponse> future = new CompletableFuture<>();
        getTeamsAndGroupsBridge().removePlayerFromGroupOrTeam(payload.role.getName(), payload.role.isGroup(), payload.uuid, players -> {
            if(players == null) {
                future.complete(payload.role.isGroup() ? DiscordEventJsonResponse.INVALID_GROUP : DiscordEventJsonResponse.INVALID_TEAM);
                return;
            }

            ConnJson.SyncedRole role = getConnJson().getSyncedRole(payload.role.getName(), payload.role.isGroup());
            if(role == null) {
                getConnJson().getSyncedRoles().add(payload.role);
                role = payload.role;
            }
            role.setPlayers(players);
            writeConn();

            future.complete(DiscordEventJsonResponse.toJson(players));
        });
        return future;
    }
}
