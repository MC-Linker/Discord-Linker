package me.lianecx.discordlinker.common.abstraction;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface LinkerServer {
    String COMMAND_NO_OUTPUT_SUCCESS = "Command executed successfully with no output.";
    String COMMAND_NO_OUTPUT_FAIL = "Command failed to execute with no output.";

    String getServerName();

    int getMaxPlayers();

    int getOnlinePlayersCount();

    List<LinkerPlayer> getOnlinePlayers();

    /**
     * Gets the absolute path to the data folder of this plugin/mod.
     */
    String getDataFolder();

    /**
     * Gets the player by the given UUID, only if they are online.
     * Returns null if the player is not online.
     */
    @Nullable LinkerPlayer getPlayer(UUID uuid);

    /**
     * Gets the player by the given name, regardless if they are offline or online.
     * Will try to return an online player first.
     * This method may involve a blocking web request to get the UUID for the given name.
     * This will return an object even if the player does not exist. To this method, all players will exist.
     * Only returns null, if an error occurred.
     */
    @Nullable LinkerOfflinePlayer getOfflinePlayer(String username);

    /**
     * Gets the player by the given UUID, regardless if they are offline or online.
     * Will try to return an online player first.
     * This method may involve a blocking web request to get the UUID for the given name.
     * Returns null if it could not fetch the player's name or if an error occurred.
     */
    @Nullable LinkerOfflinePlayer getOfflinePlayer(UUID uuid);

    void broadcastMessage(String message);

    void broadcastMessageWithClickableURLs(String message);

    boolean isPluginOrModEnabled(String pluginOrModName);

    /**
     * Checks if the server has online-mode enabled.
     */
    boolean isOnline();

    String getMinecraftVersion();

    /**
     * Gets the absolute path to the world folder.
     */
    String getWorldPath();

    /**
     * Gets the absolute path to the world container folder.
     */
    String getWorldContainerPath();

    /**
     * Gets the Floodgate prefix for Bedrock players, or null if Floodgate is not installed.
     */
    @Nullable String getFloodgatePrefix();

    /**
     * Runs a command on the server console and returns the output as a string.
     *
     * @return A CompletableFuture that will complete with the command output.
     * If there is no output, the output depends on the success: COMMAND_NO_OUTPUT_SUCCESS or COMMAND_NO_OUTPUT_FAIL.
     */
    CompletableFuture<String> executeCommand(String command);

    CompletableFuture<List<String>> getCommandCompletions(String partialCommand, int limit);
}