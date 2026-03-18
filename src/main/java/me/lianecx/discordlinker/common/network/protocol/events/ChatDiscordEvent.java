package me.lianecx.discordlinker.common.network.protocol.events;

import com.google.gson.JsonObject;
import me.lianecx.discordlinker.common.abstraction.LinkerOfflinePlayer;
import me.lianecx.discordlinker.common.abstraction.LinkerPlayer;
import me.lianecx.discordlinker.common.network.protocol.payloads.ChatPayload;
import me.lianecx.discordlinker.common.network.protocol.payloads.InvalidPayloadException;
import me.lianecx.discordlinker.common.network.protocol.responses.DiscordEventResponse;
import me.lianecx.discordlinker.common.util.JsonUtil;
import me.lianecx.discordlinker.common.util.MinecraftChatColor;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getConfig;
import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getServer;
import static me.lianecx.discordlinker.common.util.MarkdownUtil.*;
import static me.lianecx.discordlinker.common.util.UrlParser.*;

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

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%message%", markdownToColorCodes(msg));
        placeholders.put("%username%", username != null ? username : "");
        placeholders.put("%reply_message%", replyMsg != null ? markdownToColorCodes(replyMsg) : "");
        placeholders.put("%reply_username%", replyUsername != null ? replyUsername : "");

        if(replyMsg != null) {
            String reducedReplyMsg = reduceMessage(replyMsg, 30);
            placeholders.put("%reply_message_reduced%", markdownToColorCodes(reducedReplyMsg));
        }
        else placeholders.put("%reply_message_reduced%", "");

        chatMessage = applyTemplate(chatMessage, placeholders);

        //Translate color codes
        chatMessage = MinecraftChatColor.translateAlternateColorCodes(chatMessage, '&');

        if(privateMsg) {
            LinkerOfflinePlayer player = getServer().getOfflinePlayer(targetUsername);
            if(!(player instanceof LinkerPlayer)) return DiscordEventResponse.PLAYER_NOT_ONLINE;
            ((LinkerPlayer) player).sendMessageWithClickableURLs(chatMessage);
        }
        else {
            getServer().broadcastMessageWithClickableURLs(chatMessage);
        }

        return DiscordEventResponse.SUCCESS;
    }

    public static String reduceMessage(String replyMsg, int limit) {
        if (replyMsg.length() <= limit) return replyMsg;

        Matcher matcher = URL_PATTERN.matcher(replyMsg);
        if (matcher.find()) {
            int urlStart = matcher.start();
            int urlEnd = matcher.end();

            // If URL starts at 0 OR overlaps the first 30 chars
            if (urlStart == 0 || urlStart < limit) {
                int cutIndex = Math.max(limit, urlEnd);
                return replyMsg.substring(0, cutIndex) + "...";
            }
        }

        // Normal cut
        return replyMsg.substring(0, limit) + "...";
    }

    private static String applyTemplate(String template, Map<String, String> placeholders) {
        if(template == null || template.isEmpty()) return "";

        StringBuilder rendered = new StringBuilder(template.length() + 32);
        int index = 0;

        while(index < template.length()) {
            int start = template.indexOf('%', index);
            if(start == -1) {
                rendered.append(template, index, template.length());
                break;
            }

            int end = template.indexOf('%', start + 1);
            if(end == -1) {
                rendered.append(template, index, template.length());
                break;
            }

            rendered.append(template, index, start);
            String token = template.substring(start, end + 1);
            String replacement = placeholders.get(token);
            if(replacement != null) rendered.append(replacement);
            else rendered.append(token);

            index = end + 1;
        }

        return rendered.toString();
    }
}
