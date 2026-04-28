package me.lianecx.discordlinker.common.network.protocol.events;

import com.google.gson.JsonObject;
import me.lianecx.discordlinker.common.network.protocol.payloads.EmptyPayload;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventResponse;
import me.lianecx.discordlinker.common.util.JsonUtil;

import java.util.List;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getTeamsAndGroupsBridge;

public class ListTeamsAndGroupsDiscordEvent implements LinkerScheduledSyncDiscordEvent<EmptyPayload> {

    @Override
    public EmptyPayload decode(Object[] objects) {
        return new EmptyPayload();
    }

    @Override
    public DiscordEventResponse handle(EmptyPayload payload) {
        List<String> groups = getTeamsAndGroupsBridge().listGroups();
        List<String> teams = getTeamsAndGroupsBridge().listTeams();

        JsonObject responseData = new JsonObject();
        responseData.add("groups", JsonUtil.toJson(groups));
        responseData.add("teams", JsonUtil.toJson(teams));
        return new DiscordEventResponse(responseData);
    }
}
