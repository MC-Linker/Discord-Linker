package me.lianecx.discordlinker.common.network.protocol.events;

import me.lianecx.discordlinker.common.network.protocol.payloads.InvalidPayloadException;
import me.lianecx.discordlinker.common.network.protocol.payloads.SyncedRolePayload;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventResponse;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.*;

public class RemoveSyncedRoleDiscordEvent implements LinkerSyncDiscordEvent<SyncedRolePayload> {

    @Override
    public SyncedRolePayload decode(Object[] objects) throws InvalidPayloadException {
        return SyncedRolePayload.decode(objects);
    }

    @Override
    public DiscordEventResponse handle(SyncedRolePayload payload) {
        if(getConnJson() == null) return DiscordEventResponse.CONN_JSON_MISSING;
        getConnJson().getSyncedRoles().removeIf(channel -> channel.getId().equals(payload.role.getId()));
        getConnJson().write();

        if(!getConnJson().hasTeamSyncedRole()) getTeamsAndGroupsBridge().stopTeamCheck();

        return DiscordEventResponse.toJson(getConnJson().getSyncedRoles());
    }
}
