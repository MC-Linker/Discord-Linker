package me.lianecx.discordlinker.common.network.protocol.responses;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.lianecx.discordlinker.common.util.JsonUtil;

public class DiscordEventJsonResponse implements DiscordEventResponse {

    public static final DiscordEventJsonResponse INVALID_PATH = new DiscordEventJsonResponse(JsonStatus.ERROR, "Invalid Path");
    public static final DiscordEventJsonResponse INVALID_PLAYER = new DiscordEventJsonResponse(JsonStatus.ERROR, "Target Player does not exist or is not online");
    public static final DiscordEventJsonResponse INVALID_TEAM = new DiscordEventJsonResponse(JsonStatus.ERROR, "Target Team does not exist");
    public static final DiscordEventJsonResponse INVALID_GROUP = new DiscordEventJsonResponse(JsonStatus.ERROR, "Target Group does not exist");
    public static final DiscordEventJsonResponse UNKNOWN_EVENT = new DiscordEventJsonResponse(JsonStatus.ERROR, "Unknown Event");
    public static final DiscordEventJsonResponse ERROR_WRITE_CONN = new DiscordEventJsonResponse(JsonStatus.ERROR, "Error writing ConnJson");
    public static final DiscordEventJsonResponse IO_ERROR = new DiscordEventJsonResponse(JsonStatus.ERROR, "IO Error");
    public static final DiscordEventResponse NBT_ERROR = new DiscordEventJsonResponse(JsonStatus.ERROR, "Could not retrieve NBT data for player");
    public static final DiscordEventResponse CONN_JSON_MISSING = new DiscordEventJsonResponse(JsonStatus.ERROR, "ConnJson is missing");
    public static final DiscordEventResponse LUCKPERMS_NOT_LOADED = new DiscordEventJsonResponse(JsonStatus.ERROR, "LuckPerms is not loaded");
    public static final DiscordEventJsonResponse SUCCESS = new DiscordEventJsonResponse(JsonStatus.SUCCESS, "Success");

    private final JsonObject data;

    public DiscordEventJsonResponse(JsonStatus status, String message) {
        this.data = new JsonObject();
        this.data.addProperty("status", status.name().toLowerCase());
        this.data.addProperty("data", message);
    }

    public DiscordEventJsonResponse(JsonStatus status, JsonElement additionalData) {
        this.data = new JsonObject();
        this.data.addProperty("status", status.name().toLowerCase());
        this.data.add("data", additionalData);
    }

    public DiscordEventJsonResponse(JsonObject data) {
        this.data = data;
    }

    public static DiscordEventJsonResponse toJson(Object object) {
        JsonElement jsonElement = JsonUtil.toJson(object);
        if(jsonElement == null) {
            return new DiscordEventJsonResponse(JsonStatus.ERROR, "Could not serialize object to JSON");
        }
        return new DiscordEventJsonResponse(JsonStatus.SUCCESS, jsonElement);
    }

    public JsonObject getData() {
        return data;
    }

    public enum JsonStatus {
        SUCCESS,
        ERROR
    }
}