package me.lianecx.discordlinker.network;

import com.google.gson.JsonObject;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public enum Route {

    GET_FILE("/file/get", "get-file", Router::getFile),
    PUT_FILE("/file/put", "put-file", null),
    LIST_FILE("/file/list", "list-file", Router::listFile),
    VERIFY_GUILD("/verify/guild", null, Router::verifyGuild, false),
    VERIFY_USER("/verify/user", "verify-user", Router::verifyUser),
    COMMAND("/command", "command", Router::command),
    CHAT("/chat", "chat", Router::chat),
    DISCONNECT("/disconnect", "disconnect", Router::disconnect),
    CONNECT("/connect", null, Router::connect, false),
    CHANNEL_REMOVE("/channel/remove", "remove-channel", Router::removeChannel),
    CHANNEL_ADD("/channel/add", "add-channel", Router::addChannel),
    LIST_PLAYERS("/players", "list-players", Router::listPlayers),
    ROOT("/", null, Router::root, false, false);

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
