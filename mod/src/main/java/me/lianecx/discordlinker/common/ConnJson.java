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

        private List<ChatChannelType> types;
        private String webhook;
        private String id;
        private boolean allowDiscordToMinecraft;

        public List<ChatChannelType> getTypes() {
            return types;
        }

        public String getWebhook() {
            return webhook;
        }

        public String getId() {
            return id;
        }

        public boolean allowsDiscordToMinecraft() {
            return allowDiscordToMinecraft;
        }

        public enum ChatChannelType {
            CHAT,
            JOIN,
            QUIT,
            ADVANCEMENT,
            DEATH,
            PLAYER_COMMAND,
            CONSOLE_COMMAND,
            BLOCK_COMMAND,
            START,
            CLOSE;

            @Override
            public String toString() {
                return name().toLowerCase().replace('_', '-');
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
            @SerializedName("members") MEMBERS,
            @SerializedName("online") ONLINE,
            @SerializedName("offline") OFFLINE;

            @Override
            public String toString() {
                return name().toLowerCase();
            }
        }
    }

    public static class SyncedRole {
        private String id;
        private boolean isGroup; // true if group, false if team
        private String name;
        private List<String> players; // List of player UUIDs

        public String getId() {
            return id;
        }

        public boolean isGroup() {
            return isGroup;
        }

        public String getName() {
            return name;
        }

        public List<String> getPlayers() {
            return players;
        }
    }
}
