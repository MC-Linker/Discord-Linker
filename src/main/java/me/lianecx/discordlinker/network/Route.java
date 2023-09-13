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
    GET_PLAYER_NBT("/player/nbt", "get-player-nbt", Router::getPlayerNBT),
    CHAT("/chat", "chat", Router::chat),
    DISCONNECT("/disconnect", "disconnect", Router::disconnect),
    CONNECT("/connect", null, Router::connect, false),
    CHAT_CHANNEL_REMOVE("/channel/remove", "remove-channel", Router::removeChatChannel),
    CHAT_CHANNEL_ADD("/channel/add", "add-channel", Router::addChatChannel),
    STATS_CHANNEL_REMOVE("/stats-channel/remove", "remove-stats-channel", Router::removeStatsChannel),
    STATS_CHANNEL_ADD("/stats-channel/add", "add-stats-channel", Router::addStatsChannel),
    SYNCED_ROLE_ADD("/synced-role/add", "add-synced-role", Router::addSyncedRole),
    SYNCED_ROLE_REMOVE("/synced-role/remove", "remove-synced-role", Router::removeSyncedRole),
    SYNCED_ROLE_UPDATE("/synced-role/update", "update-synced-role", Router::updateSyncedRole),
    LIST_PLAYERS("/players", "list-players", Router::listPlayers),
    LIST_TEAMS_AND_GROUPS("/teams-and-groups", "list-teams-and-groups", Router::listGroupsAndTeams),
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
