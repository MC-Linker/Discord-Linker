package me.lianecx.discordlinker.common.network.protocol.events;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import me.lianecx.discordlinker.common.ConnJson;
import me.lianecx.discordlinker.common.network.protocol.payloads.ChatChannelPayload;
import me.lianecx.discordlinker.common.network.protocol.payloads.InvalidPayloadException;
import me.lianecx.discordlinker.common.network.protocol.payloads.SyncedRolePayload;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventJsonResponse;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventResponse;
import me.lianecx.discordlinker.common.util.JsonUtil;
import net.minecraft.world.scores.Team;

import java.util.List;
import java.util.UUID;
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
        if(getConnJson() == null) return completedFuture(DiscordEventJsonResponse.CONN_JSON_MISSING);

        if(!getServer().isPluginOrModEnabled("LuckPerms")) return completedFuture(DiscordEventJsonResponse.LUCKPERMS_NOT_LOADED);

        CompletableFuture<DiscordEventResponse> future = new CompletableFuture<>();
        getTeamsAndGroupsBridge().updateGroupOrTeamMembers(payload.role.getName(), payload.role.isGroup(), true, players -> {
            if(players == null) {
                future.complete(payload.role.isGroup() ? DiscordEventJsonResponse.INVALID_GROUP : DiscordEventJsonResponse.INVALID_TEAM);
                return;
            }

            payload.role.setPlayers(players);
            getConnJson().getSyncedRoles().add(payload.role);
            writeConn();

            // If a team synced role was added, start the team check
            boolean hasTeamSyncedRole = getConnJson().hasTeamSyncedRole();
            if(hasTeamSyncedRole) getTeamsAndGroupsBridge().startTeamCheck();

            future.complete(DiscordEventJsonResponse.toJson(getConnJson().getSyncedRoles()));
        });
        return future;
    }
}
