package me.lianecx.discordlinker.common;

import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import me.lianecx.discordlinker.common.abstraction.LinkerServer;
import me.lianecx.discordlinker.common.abstraction.core.LinkerLogger;
import me.lianecx.discordlinker.common.util.JsonUtil;
import me.lianecx.discordlinker.common.util.MinecraftChatColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.*;

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

    public @NotNull List<ChatChannel> getChatChannels() {
        if(chatChannels == null) chatChannels = new ArrayList<>();
        return chatChannels;
    }

    public @NotNull List<SyncedRole> getSyncedRoles() {
        if(syncedRoles == null) syncedRoles = new ArrayList<>();
        return syncedRoles;
    }

    public @NotNull List<StatsChannel> getStatsChannels() {
        if(statsChannels == null) statsChannels = new ArrayList<>();
        return statsChannels;
    }

    // --- Convenience methods ---

    public boolean hasTeamSyncedRole() {
        for(SyncedRole role : getSyncedRoles()) {
            if(!role.isGroup()) return true;
        }
        return false;
    }

    public boolean hasGroupSyncedRole() {
        for(SyncedRole role : getSyncedRoles()) {
            if(role.isGroup()) return true;
        }
        return false;
    }

    public boolean hasChatChannelType(ChatChannel.ChatChannelType type) {
        for(ChatChannel channel : getChatChannels()) {
            if(channel.getTypes().contains(type)) return true;
        }
        return false;
    }

    public @Nullable SyncedRole getSyncedRole(String name, boolean isGroup) {
        for(SyncedRole role : getSyncedRoles()) {
            if(role.getName().equalsIgnoreCase(name) && role.isGroup() == isGroup)
                return role;
        }
        return null;
    }

    public static boolean update(JsonObject connJson) {
        ConnJson conn = JsonUtil.parseConnJson(connJson);
        if(conn == null) return false;
        setConnJson(conn);
        return conn.write();
    }

    /**
     * Loads the connection.conn file from disk.
     * Takes in the server and logger as this is likely called during initialization.
     */
    public static @Nullable ConnJson load(LinkerServer server, LinkerLogger logger) {
        Path connJsonPath = Paths.get(server.getDataFolder(), CONNJSON_FILENAME);
        if(Files.exists(connJsonPath)) {
            try(Reader connReader = Files.newBufferedReader(Paths.get(server.getDataFolder(), CONNJSON_FILENAME))) {
                return JsonUtil.parseConnJson(connReader);
            }
            catch(IOException ignored) {
                logger.error(MinecraftChatColor.RED + "Failed to read connection.conn file! Please restart your server or reconnect in Discord using `/connect`.");
            }
            catch(JsonSyntaxException | JsonIOException e) {
                logger.error(MinecraftChatColor.RED + "Your connection data is corrupted! Please reconnect in Discord using `/connect`.");
            }
        }
        return null;
    }

    public boolean delete() {
        setConnJson(null);

        Path connJsonPath = Paths.get(getServer().getDataFolder(), CONNJSON_FILENAME);
        if(!Files.exists(connJsonPath)) return true;

        try {
            Files.delete(connJsonPath);
            return true;
        }
        catch(IOException e) {
            getLogger().error(MinecraftChatColor.RED + "Failed to delete connection data file!");
            e.printStackTrace();
            return false;
        }
    }

    public boolean write() {
        try {
            Files.createDirectories(Paths.get(getServer().getDataFolder()));
        }
        catch(IOException e) {
            getLogger().error(MinecraftChatColor.RED + "Failed to create data folder for connection data!");
            e.printStackTrace();
            return false;
        }
        try(Writer writer = new OutputStreamWriter(Files.newOutputStream(new File(getServer().getDataFolder(), CONNJSON_FILENAME).toPath()), StandardCharsets.UTF_8)) {
            writer.write(JsonUtil.toJsonString(this));
            return true;
        }
        catch(IOException e) {
            getLogger().error(MinecraftChatColor.RED + "Failed to write connection data to file!");
            e.printStackTrace();
            return false;
        }
    }

    public enum ConnProtocol {
        @SerializedName("websocket")
        WEBSOCKET;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
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
        private String[] webhooks = new String[] {};

        public String getId() {
            return id;
        }

        public List<ChatChannelType> getTypes() {
            return types;
        }

        public boolean allowsDiscordToMinecraft() {
            return allowDiscordToMinecraft;
        }

        public @NotNull String[] getWebhooks() {
            if(webhooks == null) webhooks = new String[] {};
            return webhooks;
        }

        public enum ChatChannelType {
            @SerializedName("chat")
            CHAT,
            @SerializedName("console")
            CONSOLE,
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
        private String updateTarget;
        private Map<StatsChannelEvent, String> names;

        public String getId() {
            return id;
        }

        public String getUpdateTarget() {
            return updateTarget;
        }

        public Map<StatsChannelEvent, String> getNames() {
            return names;
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
        private List<String> players = new ArrayList<>(); // List of player UUIDs
        private SyncedRoleDirection direction;

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public boolean isGroup() {
            return isGroup;
        }

        public @NotNull List<String> getPlayers() {
            if(players == null) players = new ArrayList<>();
            return players;
        }

        public void setPlayers(List<String> players) {
            this.players = players;
        }

        /**
         * Gets the sync direction for this role. Defaults to {@link SyncedRoleDirection#BOTH}
         * for backward compatibility when the field is absent.
         */
        public SyncedRoleDirection getDirection() {
            return direction != null ? direction : SyncedRoleDirection.BOTH;
        }

        /**
         * Whether changes from Discord should be applied to Minecraft (Discord → MC).
         * True when direction is {@code both} or {@code to_minecraft}.
         */
        public boolean syncsToMinecraft() {
            return getDirection() == SyncedRoleDirection.BOTH || getDirection() == SyncedRoleDirection.TO_MINECRAFT;
        }

        /**
         * Whether changes from Minecraft should be sent to Discord (MC → Discord).
         * True when direction is {@code both} or {@code to_discord}.
         */
        public boolean syncsToDiscord() {
            return getDirection() == SyncedRoleDirection.BOTH || getDirection() == SyncedRoleDirection.TO_DISCORD;
        }

        public enum SyncedRoleDirection {
            @SerializedName("both") BOTH,
            @SerializedName("to_minecraft") TO_MINECRAFT,
            @SerializedName("to_discord") TO_DISCORD
        }
    }
}
