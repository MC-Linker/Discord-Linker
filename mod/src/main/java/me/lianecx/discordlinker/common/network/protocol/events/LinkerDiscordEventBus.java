package me.lianecx.discordlinker.common.network.protocol.events;

import me.lianecx.discordlinker.common.network.protocol.payloads.DiscordEventPayload;
import me.lianecx.discordlinker.common.network.protocol.payloads.InvalidPayloadException;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventJsonResponse;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;

public class LinkerDiscordEventBus {

    private final Map<String, LinkerDiscordEvent<? extends DiscordEventPayload>> listeners = new HashMap<>();

    public LinkerDiscordEventBus() {
        listeners.put("get-file", new GetFileDiscordEvent());
        listeners.put("put-file", new PutFileDiscordEvent());
        listeners.put("list-file", new ListFileDiscordEvent());
        listeners.put("verify-user", new VerifyUserDiscordEvent());
        listeners.put("command", new CommandDiscordEvent());
        listeners.put("get-player-nbt", new GetPlayerNBTDiscordEvent());
        listeners.put("chat", new ChatDiscordEvent());
        listeners.put("disconnect", new DisconnectDiscordEvent());
        listeners.put("remove-channel", new RemoveChatChannelDiscordEvent());
        listeners.put("add-channel", new AddChatChannelDiscordEvent());
        listeners.put("remove-stats-channel", new RemoveStatsChannelDiscordEvent());
        listeners.put("add-stats-channel", new AddStatsChannelDiscordEvent());
        listeners.put("add-synced-role", new AddSyncedRoleDiscordEvent());
        listeners.put("remove-synced-role", new RemoveSyncedRoleDiscordEvent());
        listeners.put("add-synced-role-member", new AddSyncedRoleMemberDiscordEvent());
        listeners.put("remove-synced-role-member", new RemoveSyncedRoleMemberDiscordEvent());
        listeners.put("list-players", new ListPlayersDiscordEvent());
        listeners.put("list-teams-and-groups", new ListTeamsAndGroupsDiscordEvent());
    }

    public CompletableFuture<DiscordEventResponse> emit(String event, Object[] objects) {
        LinkerDiscordEvent<? extends DiscordEventPayload> discordEvent = listeners.get(event);
        if (discordEvent == null)
            return completedFuture(DiscordEventJsonResponse.UNKNOWN_EVENT);

        return emitTyped(discordEvent, objects);
    }

    private <T extends DiscordEventPayload> CompletableFuture<DiscordEventResponse> emitTyped(LinkerDiscordEvent<T> event, Object[] objects) {
        try {
            T payload = event.decode(objects);
            return event.handleAsync(payload);
        } catch (InvalidPayloadException e) {
            return completedFuture(new DiscordEventJsonResponse(DiscordEventJsonResponse.JsonStatus.ERROR, e.getMessage()));
        }
    }
}
