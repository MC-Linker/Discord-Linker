package me.lianecx.discordlinker.common.network.protocol.responses;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.lianecx.discordlinker.common.util.JsonUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

/**
 * Standardized protocol response wrapper.
 * <p>
 * Success: {@code {"status": "success", "data": <any>}}
 * <br>
 * Error:   {@code {"status": "error", "error": "<snake_case_code>"}}
 */
public class DiscordEventResponse {

    public static final DiscordEventResponse SUCCESS = new DiscordEventResponse(JsonUtil.singleton("status", "success"));
    public static final DiscordEventResponse UNKNOWN = new DiscordEventResponse(ProtocolError.UNKNOWN);
    public static final DiscordEventResponse NOT_FOUND = new DiscordEventResponse(ProtocolError.NOT_FOUND);
    public static final DiscordEventResponse PLAYER_NOT_ONLINE = new DiscordEventResponse(ProtocolError.PLAYER_NOT_ONLINE);
    public static final DiscordEventResponse LUCKPERMS_NOT_LOADED = new DiscordEventResponse(ProtocolError.LUCKPERMS_NOT_LOADED);
    public static final DiscordEventResponse INVALID_JSON = new DiscordEventResponse(ProtocolError.INVALID_JSON);
    public static final DiscordEventResponse IO_ERROR = new DiscordEventResponse(ProtocolError.IO_ERROR);
    public static final DiscordEventResponse UNKNOWN_EVENT = new DiscordEventResponse(ProtocolError.UNKNOWN_EVENT);
    public static final DiscordEventResponse NBT_ERROR = new DiscordEventResponse(ProtocolError.NBT_ERROR);
    public static final DiscordEventResponse CONN_JSON_MISSING = new DiscordEventResponse(ProtocolError.CONN_JSON_MISSING);
    public static final DiscordEventResponse RATE_LIMITED = new DiscordEventResponse(ProtocolError.RATE_LIMITED);

    private final JsonObject data;

    /**
     * Creates an error response with the given protocol error code.
     * Produces: {@code { "status": "error", "error": "<code>" }}
     */
    public DiscordEventResponse(ProtocolError error) {
        this.data = new JsonObject();
        this.data.addProperty("status", "error");
        this.data.addProperty("error", error.getCode());
    }

    /**
     * Creates a success response with the given data payload.
     * Produces: {@code { "status": "success", "data": <jsonData> }}
     */
    public DiscordEventResponse(JsonElement jsonData) {
        this.data = new JsonObject();
        this.data.addProperty("status", "success");
        this.data.add("data", jsonData);
    }

    /**
     * Wraps a raw {@link JsonObject} as-is (used for parsing incoming bot responses).
     */
    public DiscordEventResponse(JsonObject raw, boolean rawFlag) {
        this.data = raw;
    }

    /**
     * Serializes an object to JSON and wraps it in a success response.
     * Returns an {@link ProtocolError#UNKNOWN} error response if serialization fails.
     */
    public static DiscordEventResponse toJson(Object object) {
        JsonElement jsonElement = JsonUtil.toJson(object);
        if(jsonElement == null) return UNKNOWN;
        return new DiscordEventResponse(jsonElement);
    }

    /**
     * Reads a file, base64-encodes its contents, and wraps it in a success response.
     * Produces: {@code { "status": "success", "data": "<base64>" }}
     *
     * @return A success response with base64-encoded file data, or an {@link ProtocolError#IO_ERROR} error response.
     */
    public static DiscordEventResponse fromFile(String path) {
        try {
            byte[] fileBytes = Files.readAllBytes(Paths.get(path));
            String base64 = Base64.getEncoder().encodeToString(fileBytes);

            JsonObject obj = new JsonObject();
            obj.addProperty("status", "success");
            obj.addProperty("data", base64);
            return new DiscordEventResponse(obj, true);
        }
        catch(IOException e) {
            return IO_ERROR;
        }
    }

    /**
     * Checks whether this response represents a success.
     */
    public boolean isSuccess() {
        return data.has("status") && "success".equals(data.get("status").getAsString());
    }

    /**
     * Returns the {@link ProtocolError} if this is an error response, or {@code null} otherwise.
     */
    public ProtocolError getError() {
        if(data.has("error")) {
            String code = data.get("error").getAsString();
            return ProtocolError.fromCode(code);
        }
        return null;
    }

    /**
     * Checks whether this response is a rate-limit error.
     */
    public boolean isRateLimited() {
        return getError() == ProtocolError.RATE_LIMITED;
    }

    /**
     * Returns the {@code retryMs} value from the response data if this is a rate-limit error, or {@code -1} otherwise.
     */
    public long getRetryMs() {
        if(!isRateLimited()) return -1;

        JsonElement responseData = getResponseData();
        if(responseData != null && responseData.isJsonObject()) {
            JsonObject obj = responseData.getAsJsonObject();
            if(obj.has("retryMs")) return obj.get("retryMs").getAsLong();
        }
        return -1;
    }

    /**
     * Returns the {@code data} field as a {@link JsonElement}, or {@code null} if absent.
     */
    public JsonElement getResponseData() {
        if(data.has("data")) return data.get("data");
        return null;
    }

    /**
     * Returns the raw underlying {@link JsonObject}.
     */
    public JsonObject getData() {
        return data;
    }
}
