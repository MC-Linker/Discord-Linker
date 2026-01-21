package me.lianecx.discordlinker.common.network.protocol.events;

import me.lianecx.discordlinker.common.network.protocol.payloads.DiscordEventPayload;
import me.lianecx.discordlinker.common.network.protocol.payloads.InvalidPayloadException;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventJsonResponse;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventResponse;

import java.util.HashMap;
import java.util.Map;

public class LinkerDiscordEventBus {


    private final Map<String, LinkerDiscordEvent<? extends DiscordEventPayload>> listeners = new HashMap<>();

    public LinkerDiscordEventBus() {
        listeners.put("get-file", new GetFileDiscordEvent());
        listeners.put("put-file", new PutFileDiscordEvent());
    }

    public DiscordEventResponse emit(String event, Object[] objects) {
        LinkerDiscordEvent<? extends DiscordEventPayload> discordEvent = listeners.get(event);
        if (discordEvent == null)
            return DiscordEventJsonResponse.UNKNOWN_EVENT;

        return emitTyped(discordEvent, objects);
    }

    private <T extends DiscordEventPayload> DiscordEventResponse emitTyped(LinkerDiscordEvent<T> event, Object[] objects) {
        try {
            T payload = event.decode(objects);
            return event.handle(payload);
        } catch (InvalidPayloadException e) {
            return new DiscordEventJsonResponse(DiscordEventJsonResponse.JsonStatus.ERROR, e.getMessage());
        }
    }
}
