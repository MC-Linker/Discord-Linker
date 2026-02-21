package me.lianecx.discordlinker.common.network.protocol.events;

import me.lianecx.discordlinker.common.ConnJson;
import me.lianecx.discordlinker.common.network.protocol.payloads.InvalidPayloadException;
import me.lianecx.discordlinker.common.network.protocol.payloads.SyncedRolePayload;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventResponse;

import java.util.*;
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

        if(payload.role.isGroup() && !getTeamsAndGroupsBridge().isLuckPermsEnabled())
            return completedFuture(DiscordEventResponse.LUCKPERMS_NOT_LOADED);

        return getTeamsAndGroupsBridge().getPlayersInGroupOrTeam(payload.role.getName(), payload.role.isGroup()).thenApply(mcPlayers -> {
            if(mcPlayers == null) return DiscordEventResponse.NOT_FOUND;

            // Players sent by the bot (Discord role members)
            List<String> botPlayers = payload.role.getPlayers();

            // If direction allows Discord→MC sync, add bot players missing from MC
            if(payload.role.syncsToMinecraft()) {
                Set<String> mcSet = new HashSet<>(mcPlayers);
                for(String uuid : botPlayers) {
                    if(!mcSet.contains(uuid)) {
                        getTeamsAndGroupsBridge().addPlayerToGroupOrTeam(payload.role.getName(), payload.role.isGroup(), uuid);
                    }
                }

                if(payload.role.getDirection() == ConnJson.SyncedRole.SyncedRoleDirection.TO_MINECRAFT) {
                    // Remove players from MC who are not in the bot's list (since it's a one-way sync to MC)
                    for(String uuid : mcPlayers) {
                        if(!botPlayers.contains(uuid)) {
                            getTeamsAndGroupsBridge().removePlayerFromGroupOrTeam(payload.role.getName(), payload.role.isGroup(), uuid);
                        }
                    }
                }
            }

            Set<String> union = new LinkedHashSet<>();
            // If direction allows MC→Discord sync, add MC players missing from bot
            if(payload.role.syncsToDiscord()) union.addAll(mcPlayers);
            // If direction allows Discord→MC sync, add bot players (including those just added to MC)
            if(payload.role.syncsToMinecraft()) union.addAll(botPlayers);
            payload.role.setPlayers(new ArrayList<>(union));

            getConnJson().getSyncedRoles().add(payload.role);
            getConnJson().write();

            // If a team synced role was added, start the team check
            if(getConnJson().hasTeamSyncedRole()) getTeamsAndGroupsBridge().startTeamCheck();

            return DiscordEventResponse.toJson(getConnJson().getSyncedRoles());
        });
    }
}
