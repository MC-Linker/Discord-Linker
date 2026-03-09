package me.lianecx.discordlinker.common.network.protocol.events;

import me.lianecx.discordlinker.common.ConnJson;
import me.lianecx.discordlinker.common.TeamsAndGroupsBridge;
import me.lianecx.discordlinker.common.network.protocol.payloads.InvalidPayloadException;
import me.lianecx.discordlinker.common.network.protocol.payloads.SyncedRoleMemberPayload;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventResponse;

import java.util.concurrent.CompletableFuture;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.*;

public class AddSyncedRoleMemberDiscordEvent implements LinkerDiscordEvent<SyncedRoleMemberPayload> {

    @Override
    public SyncedRoleMemberPayload decode(Object[] objects) throws InvalidPayloadException {
        return SyncedRoleMemberPayload.decode(objects);
    }

    @Override
    public CompletableFuture<DiscordEventResponse> handleAsync(SyncedRoleMemberPayload payload) {
        if(getConnJson() == null) return CompletableFuture.completedFuture(DiscordEventResponse.CONN_JSON_MISSING);

        if(payload.role.isGroup() && !getTeamsAndGroupsBridge().isGroupPermissionsEnabled()) {
            return CompletableFuture.completedFuture(DiscordEventResponse.LUCKPERMS_NOT_LOADED);
        }

        if(payload.role.syncsToMinecraft()) {
            ConnJson.SyncedRole role = getConnJson().getSyncedRole(payload.role.getName(), payload.role.isGroup());
            if(role == null) return CompletableFuture.completedFuture(DiscordEventResponse.NOT_FOUND);

            // Has to been done before, otherwise NodeMutateEvent will cancel this operation
            role.getPlayers().add(payload.uuid);

            return getTeamsAndGroupsBridge().addPlayerToGroupOrTeam(payload.role.getName(), payload.role.isGroup(), payload.uuid)
                    .thenCompose(v -> TeamsAndGroupsBridge.getRoleResponseFromPayload(payload));
        }
        return TeamsAndGroupsBridge.getRoleResponseFromPayload(payload);
    }
}
