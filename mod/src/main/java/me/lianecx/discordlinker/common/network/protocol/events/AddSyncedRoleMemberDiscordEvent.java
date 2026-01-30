package me.lianecx.discordlinker.common.network.protocol.events;

import me.lianecx.discordlinker.common.abstraction.TeamsAndGroupsBridge;
import me.lianecx.discordlinker.common.network.protocol.payloads.InvalidPayloadException;
import me.lianecx.discordlinker.common.network.protocol.payloads.SyncedRoleMemberPayload;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventJsonResponse;
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
        if(getConnJson() == null) return CompletableFuture.completedFuture(DiscordEventJsonResponse.CONN_JSON_MISSING);

        if(payload.role.isGroup() && !getServer().isPluginOrModEnabled("LuckPerms")) {
            return CompletableFuture.completedFuture(DiscordEventJsonResponse.LUCKPERMS_NOT_LOADED);
        }

        getTeamsAndGroupsBridge().addPlayerToGroupOrTeam(payload.role.getName(), payload.role.isGroup(), payload.uuid);
        return TeamsAndGroupsBridge.getRoleResponseFromPayload(payload);
    }
}
