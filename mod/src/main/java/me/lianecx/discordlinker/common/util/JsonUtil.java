package me.lianecx.discordlinker.common.util;

import com.google.gson.*;
import me.lianecx.discordlinker.common.ConnJson;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

public class JsonUtil {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final JsonParser PARSER = new JsonParser();

    private static final TypeAdapter<JsonElement> strictGsonAdapter = new Gson().getAdapter(JsonElement.class);

    public static JsonObject singleton(String key, String value) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(key, value);
        return jsonObject;
    }

    public static String toJsonString(ConnJson connJson) {
        return GSON.toJson(connJson);
    }

    public static ConnJson getConnJson(String jsonString) {
        try {
            return GSON.fromJson(jsonString, ConnJson.class);
        }
        catch(Exception err) {
            return null;
        }
    }

    public static ConnJson getConnJson(JsonObject jsonObject) {
        try {
            return GSON.fromJson(jsonObject, ConnJson.class);
        }
        catch(Exception err) {
            return null;
        }
    }

    public static ConnJson getConnJson(Reader reader) {
        try {
            return GSON.fromJson(reader, ConnJson.class);
        }
        catch(Exception err) {
            return null;
        }
    }

    /**
     * Treats the first object in the given array as a JSON string and attempts to parse it into a JsonObject.
     */
    public static @Nullable JsonObject getJsonObjectFromObjects(Object[] objects) {
        if(objects.length == 0) return null;
        if(!(objects[0] instanceof String)) return null;
        return getJsonObjectFromString((String) objects[0]);
    }

    public static @Nullable JsonObject getJsonObjectFromString(String jsonString) {
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
}
