package me.lianecx.discordlinker.network;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import express.utils.Status;
import me.lianecx.discordlinker.ConsoleLogger;
import me.lianecx.discordlinker.DiscordLinker;
import me.lianecx.discordlinker.commands.VerifyCommand;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandException;
import org.bukkit.entity.Player;

import java.io.*;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.bukkit.Bukkit.getServer;

public class Router {

    public static final JsonObject INVALID_AUTH = new JsonObject();
    public static final JsonObject INVALID_PATH = new JsonObject();
    public static final JsonObject INVALID_PARAM = new JsonObject();
    public static final JsonObject INVALID_JSON = new JsonObject();
    public static final JsonObject INVALID_CODE = new JsonObject();
    public static final JsonObject INVALID_PLAYER = new JsonObject();
    public static final JsonObject ALREADY_CONNECTED = new JsonObject();
    public static final JsonObject SUCCESS = new JsonObject();
    public static final JsonObject CONNECT_RESPONSE = new JsonObject();
    public static final Gson GSON = new Gson();
    private static final ConsoleLogger cmdLogger = new ConsoleLogger();
    private static final String URL_REGEX = "https?://[-\\w_.]{2,}\\.[a-z]{2,4}/\\S*?";
    private static final String MD_URL_REGEX = "(?i)\\[([^]]+)]\\((" + URL_REGEX + ")\\)";
    private static String verifyCode = null;

    public static void init() throws IOException {
        INVALID_AUTH.addProperty("message", "Invalid Authorization");
        INVALID_PATH.addProperty("message", "Invalid Path");
        INVALID_PARAM.addProperty("message", "Invalid method parameter");
        INVALID_JSON.addProperty("message", "Invalid JSON");
        INVALID_PLAYER.addProperty("message", "Target player does not exist or is not online");
        ALREADY_CONNECTED.addProperty("message", "This plugin is already connected with a different guild.");
        INVALID_CODE.addProperty("message", "Invalid verification code");

        CONNECT_RESPONSE.addProperty("version", getServer().getBukkitVersion().split("-")[0]);
        CONNECT_RESPONSE.addProperty("online", getServer().getOnlineMode());
        CONNECT_RESPONSE.addProperty("worldPath", URLEncoder.encode(getWorldPath(), "utf-8"));
        CONNECT_RESPONSE.addProperty("path", URLEncoder.encode(getServer().getWorldContainer().getCanonicalPath(), "utf-8"));

        Logger log = (Logger) LogManager.getRootLogger();
        log.addAppender(cmdLogger);
    }

    public static void getFile(JsonObject data, Consumer<RouterResponse> callback) {
        try {
            File file = new File(URLDecoder.decode(data.get("path").getAsString(), "utf-8"));
            if(!file.isFile()) {
                callback.accept(new RouterResponse(Status._404, INVALID_PATH.toString()));
                return;
            }

            callback.accept(new RouterResponse(Status._200, file.toString(), true));
        }
        catch(UnsupportedEncodingException err) {
            JsonObject error = new JsonObject();
            error.addProperty("message", err.toString());
            callback.accept(new RouterResponse(Status._500, error.toString()));
        }
    }

    public static void putFile(JsonObject data, InputStream fileStream, Consumer<RouterResponse> callback) {
        try(FileOutputStream outputStream = new FileOutputStream(URLDecoder.decode(data.get("path").getAsString(), "utf-8"))) {
            //Transfer body (inputStream) to outputStream
            byte[] buf = new byte[8192];
            int length;
            while((length = fileStream.read(buf)) > 0) {
                outputStream.write(buf, 0, length);
            }

            callback.accept(new RouterResponse(Status._200, SUCCESS.toString()));
        }
        catch(IOException err) {
            JsonObject error = new JsonObject();
            error.addProperty("message", err.toString());
            callback.accept(new RouterResponse(Status._500, error.toString()));
        }
    }

    public static void listFile(JsonObject data, Consumer<RouterResponse> callback) {
        try {
            Path folder = Paths.get(URLDecoder.decode(data.get("folder").getAsString(), "utf-8"));

            JsonArray content = new JsonArray();
            Stream<Path> stream = Files.list(folder);
            stream.map(path -> {
                JsonObject object = new JsonObject();
                object.addProperty("name", path.toFile().getName());
                object.addProperty("isDirectory", path.toFile().isDirectory());
                return object;
            }).forEach(content::add);
            stream.close();

            callback.accept(new RouterResponse(Status._200, content.toString()));
        }
        catch(InvalidPathException err) {
            callback.accept(new RouterResponse(Status._404, INVALID_PATH.toString()));
        }
        catch(IOException err) {
            callback.accept(new RouterResponse(Status._200, new JsonArray().toString()));
        }
    }

    public static void verifyGuild(JsonObject data, Consumer<RouterResponse> callback) {
        //If plugin is already connected to a different guild, respond with 409: Conflict
        if(DiscordLinker.getConnJson() != null && !DiscordLinker.getConnJson().get("id").getAsString().equals(data.get("id").getAsString())) {
            callback.accept(new RouterResponse(Status._409, ALREADY_CONNECTED.toString()));
            return;
        }

        verifyCode = RandomStringUtils.randomAlphanumeric(6);
        DiscordLinker.getPlugin().getLogger().info(ChatColor.YELLOW + "Verification Code: " + verifyCode);

        getServer().getScheduler().runTaskLater(DiscordLinker.getPlugin(), () -> verifyCode = null, 3600);
        callback.accept(new RouterResponse(Status._200, SUCCESS.toString()));
    }

    public static void verifyUser(JsonObject data, Consumer<RouterResponse> callback) {
        VerifyCommand.addPlayerToVerificationQueue(UUID.fromString(data.get("uuid").getAsString()), data.get("code").getAsString());
        callback.accept(new RouterResponse(Status._200, SUCCESS.toString()));
    }

    public static void command(JsonObject data, Consumer<RouterResponse> callback) {
        Bukkit.getScheduler().runTask(DiscordLinker.getPlugin(), () -> {
            JsonObject responseJson = new JsonObject();

            try {
                String cmd = URLDecoder.decode(data.get("cmd").getAsString(), "utf-8");
                DiscordLinker.getPlugin().getLogger().info(ChatColor.AQUA + "Command from Discord: /" + cmd);
                cmdLogger.startLogging();
                getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd);
            }
            catch(UnsupportedEncodingException | IllegalArgumentException | CommandException err) {
                responseJson.addProperty("message", err.toString());
                callback.accept(new RouterResponse(Status._500, responseJson.toString()));
                return;
            }
            finally {
                cmdLogger.stopLogging();
            }

            String commandResponse = String.join("\n", cmdLogger.getData());
            cmdLogger.clearData();

            //cmdLogger returns weirdly encoded chars (probably ASCII) which is why ChatColor.stripColor does not work on the returned string.
            //Color codes are stripped successfully with this regex, but other non-english chars like € or © are not displayed correctly (�) after sending them to Discord.
            String colorCodeRegex = "(?i)\\x7F([\\dA-FK-OR])";

            //Get first color used in string
            ChatColor firstColor = null;
            Pattern pattern = Pattern.compile(colorCodeRegex);
            Matcher matcher = pattern.matcher(commandResponse);
            if(matcher.find()) firstColor = ChatColor.getByChar(matcher.group(1));

            responseJson.addProperty("message", matcher.replaceAll(""));
            if(firstColor != null) responseJson.addProperty("color", firstColor.getChar());

            callback.accept(new RouterResponse(Status._200, responseJson.toString()));
        });
    }

    public static void chat(JsonObject data, Consumer<RouterResponse> callback) {
        String msg;
        String username;
        String replyMsg;
        String replyUsername = "";
        boolean privateMsg;
        String targetUsername = "";
        try {
            msg = data.get("msg").getAsString();
            replyMsg = data.get("reply_msg") != null && !data.get("reply_msg").isJsonNull() ? data.get("reply_msg").getAsString() : null;
            username = data.get("username").getAsString();
            privateMsg = data.get("private").getAsBoolean();
            if(replyMsg != null) replyUsername = data.get("reply_username").getAsString();
            if(privateMsg) targetUsername = data.get("target").getAsString();
        }
        catch(ClassCastException err) {
            callback.accept(new RouterResponse(Status._400, INVALID_JSON.toString()));
            return;
        }

        //Get config string and insert message
        String configPath;
        if(privateMsg) configPath = "private_message";
        else if(replyMsg != null) configPath = "reply_message";
        else configPath = "message";
        String chatMessage = DiscordLinker.getPlugin().getConfig().getString(configPath);
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
        chatMessage = ChatColor.translateAlternateColorCodes('&', chatMessage);

        //Insert username
        chatMessage = chatMessage.replaceAll("%username%", username);

        //Make links clickable
        Pattern mdUrlPattern = Pattern.compile(MD_URL_REGEX);

        ComponentBuilder chatBuilder = new ComponentBuilder("");

        StringBuilder tempMessage = new StringBuilder();
        for(String word : chatMessage.split(" ")) {
            if(word.matches(URL_REGEX)) {
                if(tempMessage.length() != 0)
                    chatBuilder.append(tempMessage.toString(), ComponentBuilder.FormatRetention.NONE);
                tempMessage.setLength(0); //Clear tempMessage

                chatBuilder.append(word);
                chatBuilder.event(new ClickEvent(ClickEvent.Action.OPEN_URL, word));
                chatBuilder.underlined(true);
                tempMessage.append(" ");
            }
            else if(word.matches(MD_URL_REGEX)) {
                if(tempMessage.length() != 0)
                    chatBuilder.append(tempMessage.toString(), ComponentBuilder.FormatRetention.NONE);
                tempMessage.setLength(0); //Clear tempMessage

                Matcher matcher = mdUrlPattern.matcher(word);
                //noinspection ResultOfMethodCallIgnored
                matcher.find();

                chatBuilder.append(matcher.group(1));
                chatBuilder.event(new ClickEvent(ClickEvent.Action.OPEN_URL, matcher.group(2)));
                chatBuilder.underlined(true);
                tempMessage.append(" ");
            }
            else {
                tempMessage.append(word).append(" ");
            }
        }
        if(tempMessage.length() != 0) chatBuilder.append(tempMessage.toString(), ComponentBuilder.FormatRetention.NONE);

        if(privateMsg) {
            Player player = getServer().getPlayer(targetUsername);
            if(player == null) {
                callback.accept(new RouterResponse(Status._422, INVALID_PLAYER.toString()));
                return;
            }

            player.spigot().sendMessage(chatBuilder.create());
        }
        else {
            getServer().spigot().broadcast(chatBuilder.create());
        }

        callback.accept(new RouterResponse(Status._200, SUCCESS.toString()));
    }

    public static void disconnect(JsonObject data, Consumer<RouterResponse> callback) {
        boolean deleted = DiscordLinker.getPlugin().disconnect();

        if(deleted) {
            DiscordLinker.getPlugin().getLogger().info("Disconnected from discord...");
            callback.accept(new RouterResponse(Status._200, SUCCESS.toString()));
        }
        else {
            JsonObject error = new JsonObject();
            error.addProperty("message", "Could not delete connection file");
            callback.accept(new RouterResponse(Status._500, error.toString()));
        }
    }

    public static void connect(JsonObject data, Consumer<RouterResponse> callback) {
        try {
            DiscordLinker.getPlugin().getLogger().info("Connection request...");

            if(!data.get("code").getAsString().equals(verifyCode)) {
                DiscordLinker.getPlugin().getLogger().info("Connection unsuccessful");
                callback.accept(new RouterResponse(Status._401, INVALID_CODE.toString()));
                return;
            }

            //Create random 32-character hex string
            String token = new BigInteger(130, new SecureRandom()).toString(16);

            JsonObject connJson = new JsonObject();
            connJson.add("channels", new JsonArray());
            connJson.add("id", data.get("id"));
            connJson.add("ip", data.get("ip"));
            connJson.addProperty("protocol", "http");
            connJson.addProperty("hash", createHash(token));

            DiscordLinker.getPlugin().updateConn(connJson);

            DiscordLinker.getPlugin().getLogger().info("Successfully connected with discord server. ID: " + data.get("id").getAsString());

            JsonObject respJson = deepCopy(CONNECT_RESPONSE);
            respJson.addProperty("token", token);

            callback.accept(new RouterResponse(Status._200, respJson.toString()));
        }
        catch(IOException | NoSuchAlgorithmException err) {
            DiscordLinker.getPlugin().getLogger().info("Connection unsuccessful");
            callback.accept(new RouterResponse(Status._500, err.toString()));
        }
        finally {
            verifyCode = null;
        }
    }

    public static void removeChannel(JsonObject data, Consumer<RouterResponse> callback) {
        try {
            JsonObject connJson = DiscordLinker.getConnJson();
            JsonArray channels = connJson.get("channels").getAsJsonArray();

            for(JsonElement channel : channels) {
                if(channel.getAsJsonObject().get("id").getAsString().equals(data.get("id").getAsString())) {
                    channels.remove(channel);
                    break;
                }
            }

            //Update connJson with new channels
            connJson.add("channels", channels);
            DiscordLinker.getPlugin().updateConn(connJson);

            callback.accept(new RouterResponse(Status._200, channels.toString()));
        }
        catch(IOException err) {
            JsonObject error = new JsonObject();
            error.addProperty("message", err.toString());
            callback.accept(new RouterResponse(Status._500, error.toString()));
        }
    }

    public static void addChannel(JsonObject data, Consumer<RouterResponse> callback) {
        try {
            JsonObject connJson = DiscordLinker.getConnJson();
            JsonArray channels = connJson.get("channels").getAsJsonArray();

            //Remove channels with the same id as added channel
            for(JsonElement jsonChannel : channels) {
                if(jsonChannel.getAsJsonObject().get("id").getAsString().equals(data.get("id").getAsString())) {
                    channels.remove(jsonChannel);
                    break;
                }
            }
            channels.add(data);

            //Update connJson with new channels
            connJson.add("channels", channels);
            DiscordLinker.getPlugin().updateConn(connJson);

            callback.accept(new RouterResponse(Status._200, channels.toString()));
        }
        catch(IOException err) {
            JsonObject error = new JsonObject();
            error.addProperty("message", err.toString());
            callback.accept(new RouterResponse(Status._500, error.toString()));
        }
    }

    public static void listPlayers(JsonObject data, Consumer<RouterResponse> callback) {
        List<String> onlinePlayers = getServer().getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
        callback.accept(new RouterResponse(Status._200, GSON.toJson(onlinePlayers)));
    }

    public static void root(JsonObject data, Consumer<RouterResponse> callback) {
        callback.accept(new RouterResponse(Status._200, "To invite MC Linker, open this link: https://top.gg/bot/712759741528408064"));
    }

    private static String markdownToColorCodes(String markdown) {
        //Format **bold**
        markdown = markdown.replaceAll("\\*\\*(.+?)\\*\\*", "&l$1&r");
        //Format __underline__
        markdown = markdown.replaceAll("__(.+?)__", "&n$1&r");
        //Format *italic* and _italic_
        markdown = markdown.replaceAll("_(.+?)_|\\*(.+?)\\*", "&o$1$2&r");
        //Format ~~strikethrough~~
        markdown = markdown.replaceAll("~~(.+?)~~", "&m$1&r");
        //Format ??obfuscated??
        markdown = markdown.replaceAll("\\?\\?(.+?)\\?\\?", "&k$1&r");
        //Format inline and multiline `code` blocks
        markdown = markdown.replaceAll("(?s)```[^\\n]*\\n(.+)```|```(.+)```", "&7&n$1$2&r");
        markdown = markdown.replaceAll("`(.+?)`", "&7&n$1&r");
        //Format ||spoilers||
        markdown = markdown.replaceAll("\\|\\|(.+?)\\|\\|", "&8$1&r");
        //Format '> quotes'
        markdown = markdown.replaceAll(">+ (.+)", "&7| $1&r");

        return markdown;
    }

    private static String getWorldPath() throws IOException {
        Properties serverProperties = new Properties();
        serverProperties.load(Files.newInputStream(Paths.get("server.properties")));
        String worldName = serverProperties.getProperty("level-name");

        return Paths.get(getServer().getWorldContainer().getCanonicalPath(), worldName).toString();
    }

    public static String createHash(String originalString) throws NoSuchAlgorithmException {
        final MessageDigest digest = MessageDigest.getInstance("SHA-256");
        final byte[] hashBytes = digest.digest(originalString.getBytes(StandardCharsets.UTF_8));

        StringBuilder hexString = new StringBuilder(2 * hashBytes.length);
        for(byte hashByte : hashBytes) {
            String hex = Integer.toHexString(0xff & hashByte);
            if(hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }

        return hexString.toString();
    }

    public static JsonObject deepCopy(JsonObject obj) {
        return GSON.fromJson(GSON.toJson(obj), JsonObject.class);
    }

    public static class RouterResponse {
        private final String message;
        private final Status status;
        private final boolean isAttachment;

        public RouterResponse(Status status, String message) {
            this.status = status;
            this.message = message;
            this.isAttachment = false;
        }

        public RouterResponse(Status status, String message, boolean isAttachment) {
            this.status = status;
            this.message = message;
            this.isAttachment = isAttachment;
        }

        public String getMessage() {
            return message;
        }

        public Status getStatus() {
            return status;
        }

        public boolean isAttachment() {
            return isAttachment;
        }
    }
}
