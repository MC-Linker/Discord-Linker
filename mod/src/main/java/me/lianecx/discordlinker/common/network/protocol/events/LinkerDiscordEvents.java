package me.lianecx.discordlinker.common.network.protocol.events;

import me.lianecx.discordlinker.common.network.protocol.payloads.DiscordEventPayload;
import me.lianecx.discordlinker.common.network.protocol.payloads.InvalidPayloadException;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventJsonResponse;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventResponse;

import java.util.HashMap;
import java.util.Map;

public class LinkerDiscordEvents {


    private Map<String, DiscordEvent<? extends DiscordEventPayload>> discordEventsMap = new HashMap<>();

    public LinkerDiscordEvents() {
        discordEventsMap = new HashMap<>();
        discordEventsMap.put("get-file", new GetFileDiscordEvent());
        discordEventsMap.put("put-file", new PutFileDiscordEvent());
    }

    public DiscordEventResponse handleDiscordEvent(String event, Object[] objects) {
        DiscordEvent<? extends DiscordEventPayload> discordEvent = discordEventsMap.get(event);
        if (discordEvent == null)
            return DiscordEventJsonResponse.UNKNOWN_EVENT;

        return handleTyped(discordEvent, objects);
    }

    private <T extends DiscordEventPayload> DiscordEventResponse handleTyped(DiscordEvent<T> event, Object[] objects) {
        try {
            T payload = event.decode(objects);
            return event.handle(payload);
        } catch (InvalidPayloadException e) {
            return new DiscordEventJsonResponse(DiscordEventJsonResponse.JsonStatus.ERROR, e.getMessage());
        }
    }
}
