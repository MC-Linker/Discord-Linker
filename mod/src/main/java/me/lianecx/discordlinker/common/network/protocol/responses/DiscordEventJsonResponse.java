package me.lianecx.discordlinker.common.network.protocol.responses;

import com.google.gson.JsonObject;

public class DiscordEventJsonResponse implements DiscordEventResponse {

    public static final DiscordEventJsonResponse INVALID_PATH = new DiscordEventJsonResponse(JsonStatus.ERROR, "Invalid Path");
    public static final DiscordEventJsonResponse INVALID_PLAYER = new DiscordEventJsonResponse(JsonStatus.ERROR, "Target Player does not exist or is not online");
    public static final DiscordEventJsonResponse INVALID_TEAM = new DiscordEventJsonResponse(JsonStatus.ERROR, "Target Team does not exist");
    public static final DiscordEventJsonResponse INVALID_GROUP = new DiscordEventJsonResponse(JsonStatus.ERROR, "Target Group does not exist");
    public static final DiscordEventJsonResponse UNKNOWN_EVENT = new DiscordEventJsonResponse(JsonStatus.ERROR, "Unknown Event");
    public static final DiscordEventJsonResponse ERROR_WRITE_CONN = new DiscordEventJsonResponse(JsonStatus.ERROR, "Error writing ConnJson");
    public static final DiscordEventJsonResponse IO_ERROR = new DiscordEventJsonResponse(JsonStatus.ERROR, "IO Error");
    public static final DiscordEventJsonResponse SUCCESS = new DiscordEventJsonResponse(JsonStatus.SUCCESS, "Success");
    public static final DiscordEventResponse ERROR_NBT = new DiscordEventJsonResponse(JsonStatus.ERROR, "Could not retrieve NBT data for player");

    private final JsonObject data;

    public DiscordEventJsonResponse(JsonStatus status, String message) {
        this.data = new JsonObject();
        this.data.addProperty("status", status.name().toLowerCase());
        this.data.addProperty("message", message);
    }

    public DiscordEventJsonResponse(JsonStatus status, JsonObject additionalData) {
        this.data = new JsonObject();
        this.data.addProperty("status", status.name().toLowerCase());
        for (String key : additionalData.keySet()) {
            this.data.add(key, additionalData.get(key));
        }
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