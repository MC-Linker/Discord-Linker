package me.lianecx.discordlinker.common.network.protocol.events;

import com.google.gson.JsonObject;
import me.lianecx.discordlinker.common.abstraction.LinkerOfflinePlayer;
import me.lianecx.discordlinker.common.abstraction.LinkerPlayer;
import me.lianecx.discordlinker.common.network.protocol.payloads.ChatPayload;
import me.lianecx.discordlinker.common.network.protocol.payloads.InvalidPayloadException;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventJsonResponse;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventResponse;
import me.lianecx.discordlinker.common.util.JsonUtil;
import me.lianecx.discordlinker.common.util.MinecraftChatColor;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getConfig;
import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getServer;
import static me.lianecx.discordlinker.common.util.MarkdownUtil.*;
import static me.lianecx.discordlinker.common.util.UrlParser.URL_REGEX;

public class ChatDiscordEvent implements LinkerSyncDiscordEvent<ChatPayload> {

    @Override
    public ChatPayload decode(Object[] objects) {
        JsonObject payload = JsonUtil.parseJsonObject(objects);
        if (payload == null) throw new InvalidPayloadException(objects);

        String username = payload.get("username").getAsString();
        String message = payload.get("msg").getAsString();
        String replyMessage = payload.has("reply_msg") ? payload.get("reply_msg").getAsString() : null;
        String replyUsername = payload.has("reply_username") ? payload.get("reply_username").getAsString() : null;
        boolean privateFlag = payload.has("private") && payload.get("private").getAsBoolean();
        String targetPlayer = payload.has("target") ? payload.get("target").getAsString() : null;

        return new ChatPayload(username, message, replyMessage, replyUsername, privateFlag, targetPlayer);
    }

    @Override
    public DiscordEventResponse handle(ChatPayload payload) {
        String username = payload.username;
        String msg = payload.message;
        String replyMsg = payload.replyMessage;
        String replyUsername = payload.replyUsername;
        boolean privateMsg = payload.privateFlag;
        String targetUsername = payload.targetPlayer;

        //Get config string and insert message
        String chatMessage;
        if(payload.privateFlag) chatMessage = getConfig().getTemplatePrivateMessage();
        else if(payload.replyMessage != null) chatMessage = getConfig().getTemplateReplyMessage();
        else chatMessage = getConfig().getTemplateChatMessage();

        chatMessage = chatMessage.replaceAll("%message%", markdownToColorCodes(msg));

        if(replyMsg != null) {
            chatMessage = chatMessage.replaceAll("%reply_message%", markdownToColorCodes(replyMsg));
            chatMessage = chatMessage.replaceAll("%reply_username%", replyUsername);

            String reducedReplyMsg = replyMsg.length() > 30 ? replyMsg.substring(0, 30) + "..." : replyMsg;
            //if reply message is a url, don't reduce it
            if(replyMsg.matches(URL_REGEX) || replyMsg.matches(MD_URL_REGEX)) reducedReplyMsg = replyMsg;
            chatMessage = chatMessage.replaceAll("%reply_message_reduced%", markdownToColorCodes(reducedReplyMsg));
        }

        //Translate color codes
        chatMessage = MinecraftChatColor.translateAlternateColorCodes(chatMessage, '&');

        //Insert username
        chatMessage = chatMessage.replaceAll("%username%", username);


        if(privateMsg) {
            LinkerOfflinePlayer player = getServer().getOfflinePlayer(targetUsername);
            if(!(player instanceof LinkerPlayer)) return DiscordEventJsonResponse.INVALID_PLAYER;
            ((LinkerPlayer) player).sendMessageWithClickableURLs(chatMessage);
        }
        else {
            getServer().broadcastMessageWithClickableURLs(chatMessage);
        }

        return DiscordEventJsonResponse.SUCCESS;
    }
}
