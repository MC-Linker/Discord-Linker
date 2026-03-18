package me.lianecx.discordlinker.common.network.protocol.payloads;

public class ChatPayload implements DiscordEventPayload {
    public final String username;
    public final String message;
    public final String replyMessage;
    public final String replyUsername;
    public final boolean privateFlag;
    public final String targetPlayer;

    public ChatPayload(String username, String message, String replyMessage, String replyUsername, boolean privateFlag, String targetPlayer) {
        this.username = username;
        this.message = message;
        this.replyMessage = replyMessage;
        this.replyUsername = replyUsername;
        this.privateFlag = privateFlag;
        this.targetPlayer = targetPlayer;
    }
}
