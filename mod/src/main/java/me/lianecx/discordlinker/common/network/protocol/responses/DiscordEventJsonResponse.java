package me.lianecx.discordlinker.common.network.protocol.responses;

import com.google.gson.JsonObject;

public class DiscordEventJsonResponse implements DiscordEventResponse {

    public static final DiscordEventJsonResponse INVALID_PATH = new DiscordEventJsonResponse(JsonStatus.ERROR, "Invalid Path");
    public static final DiscordEventJsonResponse UNKNOWN_EVENT = new DiscordEventJsonResponse(JsonStatus.ERROR, "Unknown Event");
    public static final DiscordEventJsonResponse ERROR_WRITE_CONN = new DiscordEventJsonResponse(JsonStatus.ERROR, "Error writing ConnJson");
    public static final DiscordEventJsonResponse SUCCESS = new DiscordEventJsonResponse(JsonStatus.SUCCESS, "Success");

    private final JsonObject data;

    public DiscordEventJsonResponse(JsonStatus status, String message) {
        this.data = new JsonObject();
        this.data.addProperty("status", status.name().toLowerCase());
        this.data.addProperty("message", message);
    }

    public DiscordEventJsonResponse(JsonObject data) {
        this.data = data;
    }

    public JsonObject getData() {
        return data;
    }

    public enum JsonStatus {
        SUCCESS,
        ERROR
    }
}