package me.lianecx.discordlinker.network;

import com.google.gson.JsonObject;

import java.util.function.Function;

public enum Route {

    GET_FILE("/file/get", "get-file", Router::getFile),
    PUT_FILE("/file/put", "put-file", null),
    LIST_FILE("/file/list", "list-file", Router::listFile),
    VERIFY_GUILD("/verify/guild", null, Router::verifyGuild),
    VERIFY_USER("/verify/user", "verify-user", Router::verifyUser),
    COMMAND("/command", "command", Router::command),
    CHAT("/chat", "chat", Router::chat),
    DISCONNECT("/disconnect", "disconnect", Router::disconnect),
    CONNECT("/connect", null, Router::connect),
    CHNANNEL_REMOVE("/channel/remove", "remove-channel", Router::removeChannel),
    CHANNEL_ADD("/channel/add", "add-channel", Router::addChannel),
    LIST_PLAYERS("/players", "list-players", Router::listPlayers),
    ROOT("/", null, Router::root);

    private final String eventName;
    private final String path;
    private final Function<JsonObject, Router.RouterResponse> function;


    Route(String routeString, String eventName, Function<JsonObject, Router.RouterResponse> function) {
        this.path = routeString;
        this.eventName = eventName;
        this.function = function;
    }

    public static Route getRouteByPath(String path) {
        for(Route route : Route.values()) {
            if(route.getPath().equals(path)) {
                return route;
            }
        }
        return null;
    }

    public static Route getRouteByEventName(String eventName) {
        for(Route route : Route.values()) {
            if(route.getEventName().equals(eventName)) {
                return route;
            }
        }
        return null;
    }

    public Router.RouterResponse execute(JsonObject data) {
        return function.apply(data);
    }

    public String getPath() {
        return path;
    }

    public String getEventName() {
        return eventName;
    }
}
