package me.lianecx.discordlinker.common.network.protocol.events;

import me.lianecx.discordlinker.common.network.protocol.payloads.InvalidPayloadException;
import me.lianecx.discordlinker.common.network.protocol.payloads.SyncedRolePayload;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventResponse;

import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static me.lianecx.discordlinker.common.DiscordLinkerCommon.*;

public class AddSyncedRoleDiscordEvent implements LinkerDiscordEvent<SyncedRolePayload> {

    @Override
    public SyncedRolePayload decode(Object[] objects) throws InvalidPayloadException {
        return SyncedRolePayload.decode(objects);
    }

    @Override
    public CompletableFuture<DiscordEventResponse> handleAsync(SyncedRolePayload payload) {
        if(getConnJson() == null) return completedFuture(DiscordEventResponse.CONN_JSON_MISSING);

        if(payload.role.isGroup() && !getServer().isPluginOrModEnabled("LuckPerms"))
            return completedFuture(DiscordEventResponse.LUCKPERMS_NOT_LOADED);

        return getTeamsAndGroupsBridge().getPlayersInGroupOrTeam(payload.role.getName(), payload.role.isGroup()).thenApply(players -> {
            if(players == null)
                return DiscordEventResponse.INVALID_GROUP_OR_TEAM;

            payload.role.setPlayers(players);
            getConnJson().getSyncedRoles().add(payload.role);
            getConnJson().write();

            // If a team synced role was added, start the team check
            boolean hasTeamSyncedRole = getConnJson().hasTeamSyncedRole();
            if(hasTeamSyncedRole) getTeamsAndGroupsBridge().startTeamCheck();

            return DiscordEventResponse.toJson(getConnJson().getSyncedRoles());
        });
    }
}
