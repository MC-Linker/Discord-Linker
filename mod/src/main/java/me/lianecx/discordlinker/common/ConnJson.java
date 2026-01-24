package me.lianecx.discordlinker.common;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConnJson {

    public static final String CONNJSON_FILENAME = "connection.conn";

    private String id;
    private String token;
    private ConnProtocol protocol;
    private RequiredRoleToJoin requiredRoleToJoin;
    @SerializedName("channels")
    private List<ChatChannel> chatChannels = new ArrayList<>();
    @SerializedName("synced-roles")
    private List<SyncedRole> syncedRoles = new ArrayList<>();
    @SerializedName("stats-channels")
    private List<StatsChannel> statsChannels = new ArrayList<>();

    public String getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public ConnProtocol getProtocol() {
        return protocol;
    }

    public @Nullable RequiredRoleToJoin getRequiredRoleToJoin() {
        return requiredRoleToJoin;
    }

    public List<ChatChannel> getChatChannels() {
        if(chatChannels == null) chatChannels = new ArrayList<>();
        return chatChannels;
    }

    public List<SyncedRole> getSyncedRoles() {
        if(syncedRoles == null) syncedRoles = new ArrayList<>();
        return syncedRoles;
    }

    public List<StatsChannel> getStatsChannels() {
        if(statsChannels == null) statsChannels = new ArrayList<>();
        return statsChannels;
    }

    // --- Convenience methods ---

    public boolean hasTeamSyncedRole() {
        for(SyncedRole role : syncedRoles) {
            if(!role.isGroup()) return true;
        }
        return false;
    }

    public SyncedRole getSyncedRole(String name, boolean isGroup) {
        for(SyncedRole role : syncedRoles) {
            if(role.getName().equalsIgnoreCase(name) && role.isGroup() == isGroup)
                return role;
        }
        return null;
    }

    public enum ConnProtocol {
        WEBSOCKET,
    }

    public static class RequiredRoleToJoin {
        private Method method;
        private List<String> roles;

        public Method getMethod() {
            return method;
        }

        public List<String> getRoles() {
            return roles;
        }

        public enum Method {
            @SerializedName("any")
            ANY,
            @SerializedName("all")
            ALL;

            @Override
            public String toString() {
                return name().toLowerCase();
            }
        }
    }

    // --- Nested classes ---
    public static class ChatChannel {

        private String id;
        private List<ChatChannelType> types;
        private boolean allowDiscordToMinecraft;
        private String webhook;

        public String getId() {
            return id;
        }

        public List<ChatChannelType> getTypes() {
            return types;
        }

        public boolean allowsDiscordToMinecraft() {
            return allowDiscordToMinecraft;
        }

        public String getWebhook() {
            return webhook;
        }

        public enum ChatChannelType {
            @SerializedName("chat")
            CHAT,
            @SerializedName("join")
            JOIN,
            @SerializedName("quit")
            QUIT,
            @SerializedName("advancement")
            ADVANCEMENT,
            @SerializedName("death")
            DEATH,
            @SerializedName("player_command")
            PLAYER_COMMAND,
            @SerializedName("console_command")
            CONSOLE_COMMAND,
            @SerializedName("block_command")
            BLOCK_COMMAND,
            @SerializedName("start")
            START,
            @SerializedName("close")
            CLOSE;

            @Override
            public String toString() {
                return name().toLowerCase();
            }
        }
    }

    public static class StatsChannel {

        private String id;
        @SerializedName("type")
        private StatsChannelType type;
        private Map<StatsChannelEvent, String> names;

        public String getId() {
            return id;
        }

        public StatsChannelType getType() {
            return type;
        }

        public Map<StatsChannelEvent, String> getNames() {
            return names;
        }

        public enum StatsChannelType {
            @SerializedName("member-counter") MEMBER_COUNTER,
            @SerializedName("status") STATUS;

            @Override
            public String toString() {
                return name().toLowerCase().replace('_', '-');
            }
        }

        public enum StatsChannelEvent {
            @SerializedName("online") ONLINE,
            @SerializedName("offline") OFFLINE,
            @SerializedName("members") MEMBERS;

            @Override
            public String toString() {
                return name().toLowerCase();
            }
        }
    }

    public static class SyncedRole {
        private String id;
        private String name;
        private boolean isGroup; // true if group, false if team
        private List<String> players; // List of player UUIDs

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public boolean isGroup() {
            return isGroup;
        }

        public List<String> getPlayers() {
            return players;
        }

        public void setPlayers(List<String> players) {
            this.players = players;
        }
    }
}
