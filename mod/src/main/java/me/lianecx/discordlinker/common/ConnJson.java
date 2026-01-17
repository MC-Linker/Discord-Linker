package me.lianecx.discordlinker.common;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;

public class ConnJson {

    public static final String CONNJSON_FILENAME = "connection.conn";

    private String id;
    private String token;
    private ConnProtocol protocol;
    @SerializedName("channels")
    private List<ChatChannel> chatChannels;
    @SerializedName("synced-roles")
    private List<SyncedRole> syncedRoles;
    @SerializedName("stats-channels")
    private List<StatsChannel> statsChannels;

    public String getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public ConnProtocol getProtocol() {
        return protocol;
    }

    public List<ChatChannel> getChatChannels() {
        return chatChannels;
    }

    public List<SyncedRole> getSyncedRoles() {
        return syncedRoles;
    }

    public List<StatsChannel> getStatsChannels() {
        return statsChannels;
    }

    public enum ConnProtocol {
        WEBSOCKET,
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

        private Map<StatChannelEvent, String> names;
        private String id;
        @SerializedName("type")
        private StatsChannelType type;

        public Map<StatChannelEvent, String> getNames() {
            return names;
        }

        public String getId() {
            return id;
        }

        public StatsChannelType getType() {
            return type;
        }

        public enum StatsChannelType {
            @SerializedName("member-counter") MEMBER_COUNTER,
            @SerializedName("status") STATUS;

            @Override
            public String toString() {
                return name().toLowerCase().replace('_', '-');
            }
        }

        public enum StatChannelEvent {
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
    }
}
