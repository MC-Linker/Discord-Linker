package me.lianecx.discordlinker.common.network.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.lianecx.discordlinker.common.ConnJson;
import me.lianecx.discordlinker.common.abstraction.LinkerPlayer;
import me.lianecx.discordlinker.common.abstraction.LinkerServer;
import me.lianecx.discordlinker.common.network.protocol.events.LinkerDiscordEventBus;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventResponse;
import me.lianecx.discordlinker.common.network.protocol.responses.HasRequiredRoleResponses;
import me.lianecx.discordlinker.common.network.protocol.responses.ProtocolError;
import me.lianecx.discordlinker.common.util.JsonUtil;
import me.lianecx.discordlinker.common.util.MinecraftChatColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static me.lianecx.discordlinker.common.DiscordLinkerCommon.*;
import static me.lianecx.discordlinker.common.network.client.WebSocketDiscordClient.DEFAULT_RECONNECTION_ATTEMPTS;
import static me.lianecx.discordlinker.common.util.URLEncoderUtil.encodeURL;

public final class ClientManager {

    public static int DEFAULT_BOT_PORT = 80;

    public static int BOT_PORT;
    public static URI BOT_URI;

    private final LinkerDiscordEventBus discordEventBus;

    private @Nullable DiscordClient client;

    /**
     * Initializes the ClientManager with the given bot port.
     * The bot port is used to determine which bot to connect to (main/test/custom bot).
     */
    public ClientManager(LinkerDiscordEventBus discordEventBus, int botPort) {
        setBotPort(botPort);
        this.discordEventBus = discordEventBus;
    }

    /**
     * Initializes the ClientManager with the given token and bot port.
     * The token is used to authenticate with the bot, and the bot port is used to determine which bot to connect to (main/test/custom bot).
     * The connection query is generated from the server's information and sent to the bot upon connection for authentication and configuration purposes.
     */
    public ClientManager(String token, int botPort, LinkerServer server, LinkerDiscordEventBus discordEventBus) {
        setBotPort(botPort);
        this.discordEventBus = discordEventBus;
        this.client = new WebSocketDiscordClient(Collections.singletonMap("token", token), getConnectionQuery(server));
    }

    public void setBotPort(int port) {
        BOT_PORT = port;
        BOT_URI = URI.create("http://api.mclinker.com:" + BOT_PORT);
    }

    public boolean isConnected() {
        return client != null && client.isConnected();
    }

    public static void checkVersion() {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(BOT_URI + "/version").openConnection();
            try(InputStream inputStream = conn.getInputStream()) {
                String latestVersion = new BufferedReader(new InputStreamReader(inputStream)).readLine();
                if(!latestVersion.equals(getConfig().getPluginVersion()))
                    getLogger().info(MinecraftChatColor.AQUA + "Please update to the latest Discord-Linker version (" + latestVersion + ") for a bug-free and feature-rich experience.");
            }
            catch(IOException ignored) {}
        }
        catch(IOException e) {
            getLogger().warn(MinecraftChatColor.YELLOW + "Could not check for updates: " + e.getMessage());
        }
    }

    /**
     * Connects to the bot with the saved token passed in the constructor.
     * After a successful connection, reconciles synced role members with the bot and syncs member stats channels.
     */
    public CompletableFuture<Boolean> reconnect() {
        if(client == null) return completedFuture(false);
        return client.connect().thenApply(connected -> {
            if(connected) {
                client.onAny(discordEventBus::emit);
                updateStatsChannel(ConnJson.StatsChannel.StatsChannelEvent.MEMBERS);
            }
            return connected;
        });
    }

    /**
     * Connects to the bot with a verification code for the first time.
     *
     * @param code The verification code to connect with.
     */
    public CompletableFuture<Boolean> connectWithCode(String code) {
        disconnect();

        //Create random 32-character hex string as token
        String token = new BigInteger(130, new SecureRandom()).toString(16);
        Map<String, String> auth = new HashMap<>();
        auth.put("code", code);
        auth.put("token", token);

        WebSocketDiscordClient tempClient = new WebSocketDiscordClient(auth, getConnectionQuery(getServer()), 5);

        CompletableFuture<Boolean> future = new CompletableFuture<>();

        //Set listeners
        tempClient.once("auth-success", data -> {
            //Code is valid, set the adapter to the new one
            tempClient.setReconnectionAttempts(DEFAULT_RECONNECTION_ATTEMPTS);
            client = tempClient;

            JsonObject dataObject = JsonUtil.parseJsonObject(data);
            if(dataObject == null) {
                future.complete(false);
                disconnect();
                return completedFuture(DiscordEventResponse.INVALID_JSON);
            }

            // Extract data field from protocol response if present
            JsonObject authData = dataObject;
            if(dataObject.has("data") && dataObject.get("data").isJsonObject())
                authData = dataObject.getAsJsonObject("data");

            //Save connection data
            JsonObject connJson = new JsonObject();
            connJson.addProperty("protocol", "websocket");
            connJson.addProperty("id", code.split(":")[0]);
            connJson.addProperty("token", token);
            connJson.add("channels", new JsonArray());
            connJson.add("synced-roles", new JsonArray());
            connJson.add("stats-channels", new JsonArray());
            if(authData.has("requiredRoleToJoin"))
                connJson.add("requiredRoleToJoin", authData.get("requiredRoleToJoin"));

            boolean writeSuccess = ConnJson.update(connJson);
            future.complete(writeSuccess);
            if(!writeSuccess) {
                disconnect();
                return completedFuture(DiscordEventResponse.IO_ERROR);
            }

            client.onAny(discordEventBus::emit);
            return completedFuture(DiscordEventResponse.SUCCESS);
        });

        tempClient.connect().thenAccept(connected -> {
            //If connected, the bot will call auth-success above
            if(connected) return;

            tempClient.disconnect();
            future.complete(false);
            //Connect to old adapter if it exists
            reconnect();
        });
        return future;
    }

    public void chat(ConnJson.ChatChannel.ChatChannelType type) {
        chat("", type, null);
    }

    public void chat(String message, ConnJson.ChatChannel.ChatChannelType type, String player) {
        if(getConnJson() == null || getConnJson().getChatChannels().isEmpty()) return;

        JsonObject payload = new JsonObject();
        payload.addProperty("message", message);
        payload.addProperty("type", type.toString());
        payload.addProperty("player", player);
        send("chat", payload);
    }

    public void updateStatsChannel(ConnJson.StatsChannel.StatsChannelEvent event) {
        if(getConnJson() == null || getConnJson().getStatsChannels().isEmpty()) return;

        JsonObject payload = new JsonObject();
        payload.addProperty("event", event.toString());
        if(event == ConnJson.StatsChannel.StatsChannelEvent.MEMBERS)
            payload.addProperty("members", getServer().getOnlinePlayers().size());

        send("update-stats-channels", payload);
    }

    public void addSyncedRoleMember(String name, boolean isGroup, UUID uuid) {
        if(getConnJson() == null || getConnJson().getSyncedRoles().isEmpty()) return;
        updateSyncedRoleMember(name, isGroup, uuid, "add");
    }

    public void removeSyncedRoleMember(String name, boolean isGroup, UUID uuid) {
        if(getConnJson() == null || getConnJson().getSyncedRoles().isEmpty()) return;
        updateSyncedRoleMember(name, isGroup, uuid, "remove");
    }

    private void updateSyncedRoleMember(String name, boolean isGroup, UUID uuid, String addOrRemove) {
        getSyncedRoleAndUpdatePlayers(name, isGroup, role -> {
            if(role == null) return;

            JsonObject payload = new JsonObject();
            payload.addProperty("id", role.getId());
            payload.addProperty("uuid", uuid.toString());
            if(addOrRemove.equals("add"))
                send("add-synced-role-member", payload);
            else if(addOrRemove.equals("remove"))
                send("remove-synced-role-member", payload);
        });
    }

    public void removeSyncedRole(String name, boolean isGroup) {
        if(getConnJson() == null || getConnJson().getSyncedRoles().isEmpty()) return;

        getSyncedRoleAndUpdatePlayers(name, isGroup, role -> {
            if(role == null) return;

            send("remove-synced-role", role);
            getConnJson().getSyncedRoles().remove(role);
            getConnJson().write();

            if(!getConnJson().hasTeamSyncedRole()) getTeamsAndGroupsBridge().stopTeamCheck();
        });
    }

    /**
     * Reconciles synced role members with the bot after a reconnect.
     * <p>
     * For each synced role, sends the current MC-side member list to the bot via a
     * {@code sync-synced-role-members} event. The bot diffs this against Discord role membership
     * and responds with players to add/remove on the MC side.
     * <p>
     * Response format: {@code { status: "success", data: { added: ["uuid1", ...], removed: ["uuid2", ...] } }}
     * <ul>
     *   <li>{@code added}: players who have the Discord role but are missing from MC → mod adds them to team/group</li>
     *   <li>{@code removed}: players the mod listed but who don't have the Discord role → mod removes them from team/group</li>
     * </ul>
     * After reconciliation, starts the team check if there are team-based synced roles.
     */
    public void reconcileSyncedRoles() {
        ConnJson conn = getConnJson();
        if(conn == null || conn.getSyncedRoles().isEmpty()) return;

        List<ConnJson.SyncedRole> roles = new ArrayList<>(conn.getSyncedRoles());
        CompletableFuture<Void> chain = completedFuture(null);
        for(ConnJson.SyncedRole role : roles) {
            chain = chain.thenCompose(v -> reconcileSyncedRoleSequentially(conn, role));
        }
        chain.thenRun(() -> {
            // Start team check if there are team-based synced roles
            if(conn.hasTeamSyncedRole()) getTeamsAndGroupsBridge().startTeamCheck();
        });
    }

    private CompletableFuture<Void> reconcileSyncedRoleSequentially(ConnJson conn, ConnJson.SyncedRole role) {
        return getTeamsAndGroupsBridge().getPlayersInGroupOrTeam(role.getName(), role.isGroup())
            .thenCompose(currentPlayers -> {
                if(currentPlayers == null) {
                    // Team/group was deleted while offline
                    getLogger().debug(MinecraftChatColor.RED + (role.isGroup() ? "Group" : "Team") + " '" + role.getName() + "' no longer exists. Removing synced role.");
                    removeSyncedRole(role.getName(), role.isGroup());
                    return completedFuture(null);
                }

                // Send current member list to bot for reconciliation
                JsonObject payload = new JsonObject();
                payload.addProperty("id", role.getId());
                JsonArray playersArray = new JsonArray();
                for(String uuid : currentPlayers) playersArray.add(uuid);
                payload.add("players", playersArray);

                CompletableFuture<Void> done = new CompletableFuture<>();
                send("sync-synced-role-members", payload, response -> {
                    if(response == null || !response.isSuccess()) {
                        if(response != null && ProtocolError.NOT_FOUND == response.getError()) {
                            // Role was deleted on Discord while offline
                            getLogger().warn(MinecraftChatColor.YELLOW + "Synced role '" + role.getName() + "' no longer exists on Discord. Removing synced role.");
                            conn.getSyncedRoles().remove(role);
                            conn.write();
                            done.complete(null);
                            return;
                        }

                        getLogger().warn(MinecraftChatColor.YELLOW + "Failed to reconcile synced role '" + role.getName() + "'.");
                        // Still update stored players to current state
                        role.setPlayers(currentPlayers);
                        conn.write();
                        done.complete(null);
                        return;
                    }

                    try {
                        JsonObject data = response.getResponseData().getAsJsonObject();
                        JsonArray added = data.has("added") ? data.getAsJsonArray("added") : new JsonArray();
                        JsonArray removed = data.has("removed") ? data.getAsJsonArray("removed") : new JsonArray();

                        CompletableFuture<Void> memberSync = completedFuture(null);
                        // Apply Discord→MC changes only if direction allows it
                        if(role.syncsToMinecraft()) memberSync = applyRoleMembershipChangesParallel(role, added, removed);

                        // Refresh the player list after reconciliation
                        memberSync
                            .thenCompose(v -> getTeamsAndGroupsBridge().getPlayersInGroupOrTeam(role.getName(), role.isGroup()))
                            .thenAccept(reconciledPlayers -> {
                                if(reconciledPlayers != null) role.setPlayers(reconciledPlayers);
                                conn.write();
                                done.complete(null);
                            });
                    }
                    catch(Exception e) {
                        getLogger().error(MinecraftChatColor.RED + "Error reconciling synced role '" + role.getName() + "': " + e.getMessage());
                        role.setPlayers(currentPlayers);
                        conn.write();
                        done.complete(null);
                    }
                });
                return done;
            });
    }

    private CompletableFuture<Void> applyRoleMembershipChangesParallel(ConnJson.SyncedRole role, JsonArray added, JsonArray removed) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        // Add players that have the Discord role but are missing from MC
        for(JsonElement uuid : added) {
            String playerUuid = uuid.getAsString();
            futures.add(getTeamsAndGroupsBridge().addPlayerToGroupOrTeam(role.getName(), role.isGroup(), playerUuid));
        }

        // Remove players from MC that don't have the Discord role
        for(JsonElement uuid : removed) {
            String playerUuid = uuid.getAsString();
            futures.add(getTeamsAndGroupsBridge().removePlayerFromGroupOrTeam(role.getName(), role.isGroup(), playerUuid));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    public void getSyncedRoleAndUpdatePlayers(String name, boolean isGroup, Consumer<ConnJson.SyncedRole> callback) {
        if(getConnJson() == null) {
            callback.accept(null);
            return;
        }

        ConnJson.SyncedRole role = getConnJson().getSyncedRole(name, isGroup);
        if(role == null) {
            callback.accept(null);
            return;
        }

        getTeamsAndGroupsBridge().getPlayersInGroupOrTeam(name, isGroup)
                .thenAccept(players -> {
                    if(players == null) {
                        callback.accept(null);
                        return;
                    }

                    //Update players in role
                    role.getPlayers().clear();
                    for(String player : players) {
                        role.getPlayers().add(player);
                    }

                    callback.accept(role);
                });
    }

    /**
     * Acknowledges that a user has run `/account connect` and successfully verified their account.
     */
    public void sendVerificationResponse(String code, String uuid) {
        JsonObject verifyJson = new JsonObject();
        verifyJson.addProperty("code", code);
        verifyJson.addProperty("uuid", uuid);

        send("verify-response", verifyJson);
    }

    public void hasRequiredRole(String uuid, Consumer<HasRequiredRoleResponses> callback) {
        // If no join requirement to join is set, return true
        if(getConnJson() == null || getConnJson().getRequiredRoleToJoin() == null) {
            callback.accept(HasRequiredRoleResponses.TRUE);
            return;
        }

        JsonObject verifyJson = new JsonObject();
        verifyJson.addProperty("uuid", uuid);

        send("has-required-role", verifyJson, response -> {
            try {
                if(response.isSuccess()) {
                    // Expected: { status: "success", data: { hasRole: true/false } }
                    JsonObject responseData = response.getResponseData().getAsJsonObject();
                    boolean hasRole = responseData.get("hasRole").getAsBoolean();
                    callback.accept(hasRole ? HasRequiredRoleResponses.TRUE : HasRequiredRoleResponses.FALSE);
                }
                else {
                    // Expected: { status: "error", error: "not_connected" | "unknown" }
                    if(ProtocolError.NOT_CONNECTED == response.getError())
                        callback.accept(HasRequiredRoleResponses.NOT_CONNECTED);
                    else callback.accept(HasRequiredRoleResponses.ERROR);
                }
            }
            catch(Exception e) {
                callback.accept(HasRequiredRoleResponses.ERROR);
            }
        });
    }

    /**
     * Tells the bot the verification code that has been shown to the user so it listens for their DM.
     */
    public void verifyUser(LinkerPlayer player, int code) {
        JsonObject verifyJson = new JsonObject();
        verifyJson.addProperty("code", String.valueOf(code));
        verifyJson.addProperty("uuid", player.getUUID());
        verifyJson.addProperty("username", player.getName());

        send("verify-user", verifyJson);
    }

    public void getInviteURL(Consumer<String> callback) {
        send("invite-url", new JsonObject(), response -> {
            try {
                if(!response.isSuccess()) {
                    callback.accept(null);
                    return;
                }

                // Expected: { status: "success", data: { url: "..." } }
                JsonObject responseData = response.getResponseData().getAsJsonObject();
                if(responseData.get("url").isJsonNull()) callback.accept(null);
                else callback.accept(responseData.get("url").getAsString());
            }
            catch(Exception e) {
                callback.accept(null);
            }
        });
    }

    /**
     * Sends an event to the bot with no data.
     */
    public void send(String eventName) {
        send(eventName, new Object[] {});
    }

    /**
     * Sends an event to the bot with a JSON object as data.
     */
    public void send(String eventName, JsonObject data) {
        send(eventName, data.toString());
    }

    /**
     * Sends an event to the bot with a JSON object as data and a callback for the response.
     */
    public void send(String eventName, JsonObject data, Consumer<DiscordEventResponse> callback) {
        if(client == null) return;
        client.send(eventName, new Object[] { data.toString() }, callback);
    }

    /**
     * Sends an event to the bot with arbitrary data.
     */
    public void send(String eventName, Object... data) {
        if(client == null) return;
        client.send(eventName, data);
    }

    public void disconnect() {
        if(client == null) return;
        client.disconnect();
    }

    public @NotNull Map<String, String> getConnectionQuery(LinkerServer server) {
        boolean isMinehut = server.isPluginOrModEnabled("MinehutPlugin");

        Map<String, String> response = new HashMap<>();
        response.put("version", server.getMinecraftVersion());
        // Minehut servers have online mode disabled in the server.properties file, because a proxy handles authentication
        response.put("online", String.valueOf(isMinehut || server.isOnline()));
        response.put("worldPath", encodeURL(server.getWorldPath()));
        response.put("path", encodeURL(server.getWorldContainerPath()));
        response.put("floodgatePrefix", server.getFloodgatePrefix());
        return response;
    }

    public void disconnectForce() {
        send("disconnect-force");
    }
}
