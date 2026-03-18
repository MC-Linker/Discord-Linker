package me.lianecx.discordlinker.common.network.protocol.responses;

/**
 * Standardized error codes for protocol responses.
 * Each enum value's snake_case code is derived from {@code name().toLowerCase()}.
 */
public enum ProtocolError {

    /** Generic/unhandled error. */
    UNKNOWN,
    /** Wrong authorization credentials. */
    UNAUTHORIZED,
    /** Requested resource/player/file not found. */
    NOT_FOUND,
    /** The targeted player is not online. */
    PLAYER_NOT_ONLINE,
    /** The LuckPerms plugin is not loaded on the server. */
    LUCKPERMS_NOT_LOADED,
    /** The plugin did not respond (timeout or no connection). */
    NO_RESPONSE,
    /** Malformed JSON in event data. */
    INVALID_JSON,
    /** Request was rate-limited. */
    RATE_LIMITED,
    /** The user is not connected/linked. */
    NOT_CONNECTED,
    /** The target group or team does not exist. */
    INVALID_GROUP_OR_TEAM,
    /** An I/O error occurred (file read/write, connection file, etc.). */
    IO_ERROR,
    /** The received Socket.IO event is not recognized. */
    UNKNOWN_EVENT,
    /** Could not retrieve NBT data for the player. */
    NBT_ERROR,
    /** The connection configuration file is missing. */
    CONN_JSON_MISSING;

    public static ProtocolError fromCode(String code) {
        try {
            return ProtocolError.valueOf(code.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }

    /**
     * Returns the snake_case error code string for use in protocol responses.
     */
    public String getCode() {
        return name().toLowerCase();
    }
}
