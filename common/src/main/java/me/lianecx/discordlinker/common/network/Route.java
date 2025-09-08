package me.lianecx.discordlinker.common.network;

import com.google.gson.JsonObject;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public enum Route {

    GET_FILE("/file/get", "get-file"),
    PUT_FILE("/file/put", "put-file", null),
    LIST_FILE("/file/list", "list-file"),
    VERIFY_USER("/verify/user", "verify-user"),
    COMMAND("/command", "command"),
    GET_PLAYER_NBT("/player/nbt", "get-player-nbt"),
    CHAT("/chat", "chat"),
    DISCONNECT("/disconnect", "disconnect"),
    CONNECT("/connect", null, Router::connect, false),
    CHAT_CHANNEL_REMOVE("/channel/remove", "remove-channel"),
    CHAT_CHANNEL_ADD("/channel/add", "add-channel"),
    STATS_CHANNEL_REMOVE("/stats-channel/remove", "remove-stats-channel"),
    STATS_CHANNEL_ADD("/stats-channel/add", "add-stats-channel"),
    SYNCED_ROLE_ADD("/synced-role/add", "add-synced-role"),
    SYNCED_ROLE_REMOVE("/synced-role/remove", "remove-synced-role"),
    SYNCED_ROLE_ADD_MEMBER("/synced-role/add-member", "add-synced-role-member"),
    SYNCED_ROLE_REMOVE_MEMBER("/synced-role/remove-member", "remove-synced-role-member"),
    LIST_PLAYERS("/players", "list-players"),
    LIST_TEAMS_AND_GROUPS("/teams-and-groups", "list-teams-and-groups"),
    ROOT("/", null, null, false, false);

    private final String eventName;
    private final String path;
    private final BiConsumer<JsonObject, Consumer<Router.RouterResponse>> function;
    private final boolean requiresToken;
    private final boolean botOnly;


    Route(String routeString, String eventName, BiConsumer<JsonObject, Consumer<Router.RouterResponse>> function) {
        this.path = routeString;
        this.eventName = eventName;
        this.function = function;
        this.requiresToken = true;
        this.botOnly = true;
    }

    Route(String routeString, String eventName, BiConsumer<JsonObject, Consumer<Router.RouterResponse>> function, boolean requiresToken) {
        this.path = routeString;
        this.eventName = eventName;
        this.function = function;
        this.requiresToken = requiresToken;
        this.botOnly = true;
    }

    Route(String routeString, String eventName, BiConsumer<JsonObject, Consumer<Router.RouterResponse>> function, boolean requiresToken, boolean botOnly) {
        this.path = routeString;
        this.eventName = eventName;
        this.function = function;
        this.requiresToken = requiresToken;
        this.botOnly = botOnly;
    }

    public static Route getRouteByPath(String path) {
        for(Route route : Route.values()) {
            if(route.getPath() != null && route.getPath().equals(path)) {
                return route;
            }
        }
        return null;
    }

    public static Route getRouteByEventName(String eventName) {
        for(Route route : Route.values()) {
            if(route.getEventName() != null && route.getEventName().equals(eventName)) {
                return route;
            }
        }
        return null;
    }

    public void execute(JsonObject data, Consumer<Router.RouterResponse> callback) {
        function.accept(data, callback);
    }

    public String getPath() {
        return path;
    }

    public String getEventName() {
        return eventName;
    }

    public boolean doesRequireToken() {
        return requiresToken;
    }

    public boolean isBotOnly() {
        return botOnly;
    }
}
