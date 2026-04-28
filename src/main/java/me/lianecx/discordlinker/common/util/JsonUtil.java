package me.lianecx.discordlinker.common.util;

import com.google.gson.*;
import me.lianecx.discordlinker.common.ConnJson;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.io.IOException;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonUtil {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final JsonParser PARSER = new JsonParser();

    private static final TypeAdapter<JsonElement> strictGsonAdapter = new Gson().getAdapter(JsonElement.class);

    private JsonUtil() {}

    public static JsonObject singleton(String key, String value) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(key, value);
        return jsonObject;
    }

    public static String toJsonString(ConnJson connJson) {
        return GSON.toJson(connJson);
    }

    public static ConnJson parseConnJson(String jsonString) {
        try {
            ConnJson conn = GSON.fromJson(jsonString, ConnJson.class);
            if(conn != null) normalizeConnJson(conn);
            return conn;
        }
        catch(Exception err) {
            return null;
        }
    }

    public static ConnJson parseConnJson(JsonObject jsonObject) {
        try {
            ConnJson conn = GSON.fromJson(jsonObject, ConnJson.class);
            if(conn != null) normalizeConnJson(conn);
            return conn;
        }
        catch(Exception err) {
            return null;
        }
    }

    public static ConnJson parseConnJson(Reader reader) {
        try {
            ConnJson conn = GSON.fromJson(reader, ConnJson.class);
            if(conn != null) normalizeConnJson(conn);
            return conn;
        }
        catch(Exception err) {
            return null;
        }
    }

    /**
     * Normalizes null lists in a ConnJson instance after Gson deserialization.
     * Gson sets fields to null when the JSON value is explicitly null, even if the field has an initializer.
     */
    private static void normalizeConnJson(ConnJson conn) {
        // Force the null-safe getters to initialize any null lists
        conn.getChatChannels();
        conn.getSyncedRoles();
        conn.getStatsChannels();
    }

    /**
     * Treats the first object in the given array as a JSON string or JSONObject and attempts to parse it into a JsonObject.
     */
    public static @Nullable JsonObject parseJsonObject(Object[] objects) {
        if(objects.length == 0) return null;
        if(objects[0] instanceof JSONObject) return parseJsonObject(objects[0].toString());
        else if(objects[0] instanceof String) return parseJsonObject((String) objects[0]);
        return null;
    }

    public static @Nullable JsonObject parseJsonObject(String jsonString) {
        try {
            JsonElement json = PARSER.parse(jsonString);
            if(!json.isJsonObject()) return null;
            return json.getAsJsonObject();
        }
        catch(Exception err) {
            return null;
        }
    }

    public static boolean isValidJson(String json) {
        try {
            strictGsonAdapter.fromJson(json);
        } catch (JsonSyntaxException | IOException e) {
            return false;
        }
        return true;
    }

    public static <T> T fromJson(JsonObject payload, Class<T> clazz) {
       try {
           return GSON.fromJson(payload, clazz);
       } catch(Exception err) {
           return null;
       }
    }

    public static JsonElement toJson(Object object) {
        try {
            return GSON.toJsonTree(object);
        } catch(Exception err) {
            return null;
        }
    }

    public static @Nullable String parseJsonPropertyFast(String arg, String property) {
        //Parse with regex
        Pattern pattern = Pattern.compile("\"" + property + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(arg);
        if(matcher.find()) return matcher.group(1);
        return null;
    }
}
