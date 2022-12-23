package me.lianecx.discordlinker;

import com.google.gson.*;
import express.Express;
import express.utils.Status;
import me.lianecx.discordlinker.commands.LinkerCommand;
import me.lianecx.discordlinker.commands.LinkerTabCompleter;
import me.lianecx.discordlinker.commands.VerifyCommand;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public final class DiscordLinker extends JavaPlugin {

    private static Express app;
    private static JsonObject connJson;
    private static DiscordLinker plugin;
    private static final ConsoleLogger cmdLogger = new ConsoleLogger();
    private static String verifyCode = null;
    private static final Gson gson = new Gson();
    FileConfiguration config = getConfig();

    @Override
    public void onEnable() {
        plugin = this;
        config.options().copyDefaults(true);
        saveConfig();

        getServer().getPluginManager().registerEvents(new ChatListeners(), this);
        getCommand("linker").setExecutor(new LinkerCommand());
        getCommand("linker").setTabCompleter(new LinkerTabCompleter());
        getCommand("verify").setExecutor(new VerifyCommand());

        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            HttpConnection.checkVersion();

            try(Reader connReader = Files.newBufferedReader(Paths.get(getDataFolder() + "/connection.conn"))) {
                JsonElement parser = new JsonParser().parse(connReader);
                connJson = parser.isJsonObject() ? parser.getAsJsonObject() : null;

                HttpConnection.sendChat("", "start", null);
            }
            catch(IOException ignored) {}
        });

        int pluginId = 17143;
        Metrics metrics = new Metrics(this, pluginId);
        metrics.addCustomChart(new SimplePie("server_connected_with_discord", () -> connJson != null ? "true" : "false"));

        Logger log = (Logger) LogManager.getRootLogger();
        log.addAppender(cmdLogger);

        app = startServer();
        getLogger().info(ChatColor.GREEN + "Plugin enabled.");
    }

    @Override
    public void onDisable() {
        HttpConnection.sendChat("", "close", null);
        getLogger().info(ChatColor.RED + "Plugin disabled.");
        getServer().getScheduler().cancelTasks(this);
        app.stop();
    }

    public static JsonObject getConnJson() {
        return connJson;
    }

    public static DiscordLinker getPlugin() {
        return plugin;
    }

    public static Express getApp() {
        return app;
    }

    public Express startServer() {
        JsonObject invalidHash = new JsonObject();
        invalidHash.addProperty("message", "Invalid Hash");
        JsonObject invalidPath = new JsonObject();
        invalidPath.addProperty("message", "Invalid Path");
        JsonObject invalidJson = new JsonObject();
        invalidJson.addProperty("message", "Invalid JSON");
        JsonObject invalidConnection = new JsonObject();
        invalidConnection.addProperty("message", "Invalid connection");

        JsonObject success = new JsonObject();
        Express app = new Express();

        //GET localhost:11111/file/get/?path=/path/to/file
        app.get("/file/get", (req, res) -> {
            if(wrongHash(req.getIp(), req.getAuthorization().get(0).getData())) {
                res.setStatus(Status._401);
                res.send(invalidHash.toString());
                return;
            }

            try {
                Path file = Paths.get(URLDecoder.decode(req.getQuery("path"), "utf-8"));

                if(!res.sendAttachment(file)) {
                    res.setStatus(Status._404);
                    res.send(invalidPath.toString());
                }
            }
            catch(InvalidPathException err) {
                res.setStatus(Status._404);
                res.send(invalidPath.toString());
            }
            catch(UnsupportedEncodingException err) {
                res.setStatus(Status._500);
                JsonObject error = new JsonObject();
                error.addProperty("message", err.toString());
                res.send(error.toString());
            }
        });

        // POST localhost:11111/file/put/
        // { fileStreamToFile }


        app.post("/file/put", (req, res) -> {
            if(wrongHash(req.getIp(), req.getAuthorization().get(0).getData())) {
                res.setStatus(Status._401);
                res.send(invalidHash.toString());
                return;
            }

            try(FileOutputStream outputStream = new FileOutputStream(URLDecoder.decode(req.getQuery("path"), "utf-8"))) {

                //Transfer body (inputStream) to outputStream
                byte[] buf = new byte[8192];
                int length;
                while((length = req.getBody().read(buf)) > 0) {
                    outputStream.write(buf, 0, length);
                }

                res.send(success.toString());
            }
            catch(IOException err) {
                res.setStatus(Status._500);
                res.send(err.toString());
            }
        });

        //GET localhost:11111/file/list/?folder="/world/"
        app.get("/file/list", (req, res) -> {
            if(wrongHash(req.getIp(), req.getAuthorization().get(0).getData())) {
                res.setStatus(Status._401);
                res.send(invalidHash.toString());
                return;
            }

            try {
                Path folder = Paths.get(URLDecoder.decode(req.getQuery("folder"), "utf-8"));

                JsonArray content = new JsonArray();
                Stream<Path> stream = Files.list(folder);
                stream.map(path -> {
                    JsonObject object = new JsonObject();
                    object.addProperty("name", path.toFile().getName());
                    object.addProperty("isDirectory", path.toFile().isDirectory());
                    return object;
                }).forEach(content::add);
                stream.close();

                res.send(content.toString());
            }
            catch (InvalidPathException err) {
                res.setStatus(Status._404);
                res.send(invalidPath.toString());
            }
            catch (IOException err) {
                res.send("[]");
            }
        });

        //GET localhost:11111/verify/guild
        app.get("/verify/guild", (req, res) -> {
            if (wrongIp(req.getIp())) {
                res.setStatus(Status._401);
                res.send(invalidConnection.toString());
                return;
            }

            //If connJson and connected guild id is not the same as the one in the request, return 409
            else if (connJson != null && !connJson.get("id").getAsString().equals(req.getQuery("id"))) {
                res.setStatus(Status._409);
                JsonObject alreadyConnected = new JsonObject();
                alreadyConnected.addProperty("message", "This plugin is already connected");
                res.send(alreadyConnected.toString());
                return;
            }

            verifyCode = RandomStringUtils.randomAlphanumeric(6);
            getLogger().info(ChatColor.YELLOW + "Verification Code: " + verifyCode);

            getServer().getScheduler().runTaskLater(this, () -> verifyCode = null, 3600);
            res.send(success.toString());
        });

        //GET localhost:11111/verify/?code=12345&uuid=12345-12345-12345-12345
        app.get("/verify/user", (req, res) -> {
            if(wrongHash(req.getIp(), req.getAuthorization().get(0).getData())) {
                res.setStatus(Status._401);
                res.send(invalidHash.toString());
                return;
            }

            VerifyCommand.addPlayerToVerificationQueue(UUID.fromString(req.getQuery("uuid")), req.getQuery("code"));
            res.send(success.toString());
        });

        //GET localhost:11111/command/?cmd=ban+Lianecx
        app.get("/command", (req, res) -> {
            if(wrongHash(req.getIp(), req.getAuthorization().get(0).getData())) {
                res.setStatus(Status._401);
                res.send(invalidHash.toString());
                return;
            }

            JsonObject responseJson = new JsonObject();

            getServer().getScheduler().runTask(this, () -> {
                try {
                    String cmd = URLDecoder.decode(req.getQuery("cmd"), "utf-8");
                    getLogger().info(ChatColor.AQUA + "Command from Discord: /" + cmd);
                    cmdLogger.startLogging();
                    getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd);
                }
                catch(UnsupportedEncodingException err) {
                    responseJson.addProperty("message", err.toString());
                    res.setStatus(Status._500);
                    res.send(responseJson.toString());
                    return;
                }
                catch(IllegalArgumentException | CommandException err) {
                    res.setStatus(Status._500);
                    responseJson.addProperty("message", err.toString());
                    res.send(responseJson.toString());
                }
                finally {
                    cmdLogger.stopLogging();
                }

                String data = String.join("\n", cmdLogger.getData());

                //cmdLogger returns weirdly encoded chars (probably ASCII) which is why ChatColor.stripColor does not work on the returned string.
                //Color codes are stripped successfully with this regex, but other non-english chars like € or © are not displayed correctly (�) after sending them to Discord.
                //Anyone can feel free to try to solve this problem
                // If you do so, please increase this counter as a warning for the next person:

                // totalHoursWastedHere = 11
                String colorCodeRegex = "(?i)\\x7F([\\dA-FK-OR])";

                //Get first color used in string
                ChatColor firstColor = null;
                Pattern pattern = Pattern.compile(colorCodeRegex);
                Matcher matcher = pattern.matcher(data);
                if(matcher.find()) firstColor = ChatColor.getByChar(matcher.group(1));

                responseJson.addProperty("message", matcher.replaceAll(""));
                if(firstColor != null) responseJson.addProperty("color", firstColor.getChar());

                res.send(responseJson.toString());
                cmdLogger.clearData();
            });
        });

//        POST localhost:11111/chat/
//            {
//                "msg": "Ayoo",
//                "username": "Lianecx,
//                "private": false
//            }

        app.post("/chat", (req, res) -> {
            if(wrongHash(req.getIp(), req.getAuthorization().get(0).getData())) {
                res.setStatus(Status._401);
                res.send(invalidHash.toString());
                return;
            }

            JsonObject parser = new JsonParser().parse(new InputStreamReader(req.getBody())).getAsJsonObject();

            String msg;
            String username;
            String replyMsg;
            boolean privateMsg;
            String targetUsername = null;
            try {
                msg = parser.get("msg").getAsString();
                replyMsg = parser.get("reply") != null && !parser.get("reply").isJsonNull() ? parser.get("reply").getAsString() : null;
                username = parser.get("username").getAsString();
                privateMsg = parser.get("private").getAsBoolean();
                if(privateMsg) targetUsername = parser.get("target").getAsString();
            }
            catch(ClassCastException err) {
                res.setStatus(Status._400);
                res.send(invalidJson.toString());
                return;
            }

            //Get config string and insert message
            String configPath;
            if(privateMsg) configPath = "private_message";
            else if(replyMsg != null) configPath = "reply_message";
            else configPath = "message";
            String chatMessage = getConfig().getString(configPath);
            chatMessage = chatMessage.replaceAll("%message%", markdownToColorCodes(msg));

            if(replyMsg != null) {
                chatMessage = chatMessage.replaceAll("%reply_message%", markdownToColorCodes(replyMsg));
                String reducedReplyMsg = replyMsg.length() > 20 ? replyMsg.substring(0, 20) + "..." : replyMsg;
                chatMessage = chatMessage.replaceAll("%reply_message_reduced%", markdownToColorCodes(reducedReplyMsg));
            }

            //Translate color codes
            chatMessage = ChatColor.translateAlternateColorCodes('&', chatMessage);

            //Insert username
            chatMessage = chatMessage.replaceAll("%username%", username);

            //Make links clickable
            String urlRegex = "https?://[-\\w_.]{2,}\\.[a-z]{2,4}/\\S*?";
            String mdUrlRegex = "(?i)\\[([^]]+)]\\((" + urlRegex + ")\\)";
            Pattern mdUrlPattern = Pattern.compile(mdUrlRegex);

            ComponentBuilder chatBuilder = new ComponentBuilder("");

            StringBuilder tempMessage = new StringBuilder();
            for(String word : chatMessage.split(" ")) {
                if(word.matches(urlRegex)) {
                    if(tempMessage.length() != 0)
                        chatBuilder.append(tempMessage.toString(), ComponentBuilder.FormatRetention.NONE);
                    tempMessage.setLength(0); //Clear tempMessage

                    chatBuilder.append(word);
                    chatBuilder.event(new ClickEvent(ClickEvent.Action.OPEN_URL, word));
                    chatBuilder.underlined(true);
                    tempMessage.append(" ");
                }
                else if(word.matches(mdUrlRegex)) {
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
            if(tempMessage.length() != 0)
                chatBuilder.append(tempMessage.toString(), ComponentBuilder.FormatRetention.NONE);

            if(privateMsg) {
                Player player = getServer().getPlayer(targetUsername);
                if(player == null) {
                    res.setStatus(Status._422);
                    JsonObject invalidPlayer = new JsonObject();
                    invalidPlayer.addProperty("message", "Target player does not exist or is not online");
                    res.send(invalidPlayer.toString());
                    return;
                }

                player.spigot().sendMessage(chatBuilder.create());
            }
            else {
                getServer().spigot().broadcast(chatBuilder.create());
            }

            res.send(success.toString());
        });

        //GET localhost:11111/disconnect/
        app.get("/disconnect", (req, res) -> {
            if(wrongHash(req.getIp(), req.getAuthorization().get(0).getData())) {
                res.setStatus(Status._401);
                res.send(invalidHash.toString());
                return;
            }

            boolean deleted = this.disconnect();
            if(deleted) {
                getLogger().info("Disconnected from discord...");
                res.send(success.toString());
            }
            else {
                res.setStatus(Status._500);
                JsonObject error = new JsonObject();
                error.addProperty("message", "Could not delete connection file");
                res.send(error.toString());
            }
        });

//        POST localhost:11111/connect/
//            {
//                "ip": ip,
//                "id": guildId
//            }

        app.post("/connect", (req, res) -> {
            getLogger().info("Connection request...");
            JsonObject parser = new JsonParser().parse(new InputStreamReader(req.getBody())).getAsJsonObject();
            String hash = req.getAuthorization().get(0).getData();
            String code = req.getAuthorization().get(1).getData();

            if(wrongConnection(req.getIp(), hash)) {
                getLogger().info("Connection unsuccessful");
                res.setStatus(Status._400);
                res.send(invalidConnection.toString());
                return;
            }
            else if(!req.hasAuthorization() || !code.equals(verifyCode)) {
                getLogger().info("Connection unsuccessful");
                res.setStatus(Status._401);

                JsonObject invalidCode = new JsonObject();
                invalidCode.addProperty("message", "Invalid verification");
                res.send(invalidCode.toString());
                return;
            }

            try {
                connJson = new JsonObject();
                connJson.addProperty("hash", createHash(hash));
                connJson.add("channels", new JsonArray());
                connJson.add("id", parser.get("id"));
                connJson.add("ip", parser.get("ip"));
                updateConn();

                getLogger().info("Successfully connected with discord server. ID: " + parser.get("id"));

                JsonObject botConnJson = new JsonObject();
                botConnJson.addProperty("hash", hash);
                botConnJson.add("id", parser.get("id"));
                botConnJson.add("ip", parser.get("ip"));
                botConnJson.addProperty("version", getServer().getBukkitVersion().split("-")[0]);
                botConnJson.addProperty("online", getServer().getOnlineMode());
                botConnJson.addProperty("worldPath", URLEncoder.encode(getWorldPath(), "utf-8"));
                botConnJson.addProperty("path", URLEncoder.encode(getServer().getWorldContainer().getCanonicalPath(), "utf-8"));

                res.send(botConnJson.toString());
            }
            catch(IOException | NoSuchAlgorithmException err) {
                getLogger().info("Connection unsuccessful");
                res.setStatus(Status._500);
                res.send(err.toString());
            }
            finally {
                verifyCode = null;
            }
        });

//        POST localhost:11111/channel/add
//            {
//                "id": channelId,
//                "types": ["chat", "close"]
//            }

        app.post("/channel/:method", (req, res) -> {
            String hash = req.getAuthorization().get(0).getData();

            if(wrongHash(req.getIp(), hash)) {
                res.setStatus(Status._401);
                res.send(invalidHash.toString());
                return;
            }

            JsonObject newChannel = new JsonParser().parse(new InputStreamReader(req.getBody())).getAsJsonObject();

            try {
                //Save channels from connJson and add new channel
                JsonArray oldChannels = new JsonArray();
                if(connJson != null && connJson.get("channels") != null)
                    oldChannels = connJson.getAsJsonArray("channels");

                JsonArray channels = new JsonArray();
                //Remove duplicates and fill new channel array
                for(JsonElement oldChannel : oldChannels) {
                    if(oldChannel.getAsJsonObject().get("id").equals(newChannel.get("id"))) continue;
                    channels.add(oldChannel);
                }

                //Add new channel if method equals to add
                if(req.getParam("method").equals("add")) channels.add(newChannel);
                else if(!req.getParam("method").equals("remove")) {
                    res.setStatus(Status._400);
                    JsonObject invalidParam = new JsonObject();
                    invalidParam.addProperty("message", "Invalid method parameter");
                    res.send(invalidParam.toString());
                    return;
                }

                //Create new connJson
                connJson.add("channels", channels);
                updateConn();

                res.send(channels.toString());
            }
            catch(IOException err) {
                res.setStatus(Status._500);
                JsonObject error = new JsonObject();
                error.addProperty("message", err.toString());
                res.send(error.toString());
            }
        });

        app.get("/players", (req, res) -> {
            if(wrongHash(req.getIp(), req.getAuthorization().get(0).getData())) {
                res.setStatus(Status._401);
                res.send(invalidHash.toString());
                return;
            }

            List<String> onlinePlayers = getServer().getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
            res.send(gson.toJson(onlinePlayers));
        });

        //GET localhost:11111/
        app.get("/", (req, res) -> res.send("To invite MC Linker, open this link: https://top.gg/bot/712759741528408064"));

        int port = config.getInt("port") != 0 ? config.getInt("port") : 11111;
        app.listen(() -> getLogger().info("Listening on port " + port), port);

        return app;
    }

    public boolean disconnect() {
        connJson = null;
        File connection = new File(getDataFolder() + "/connection.conn");
        return connection.delete();
    }

    public String getWorldPath() throws IOException {
        Properties serverProperties = new Properties();
        serverProperties.load(Files.newInputStream(Paths.get("server.properties")));
        String worldName = serverProperties.getProperty("level-name");

        return Paths.get(getServer().getWorldContainer().getCanonicalPath(), worldName).toString();
    }

    public void updateConn() throws IOException {
        FileWriter writer = new FileWriter(getDataFolder() + "/connection.conn");
        writer.write(connJson.toString());
        writer.close();
    }

    public String markdownToColorCodes(String markdown) {
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

    public boolean wrongHash(String ip, String hash) {
        if(wrongIp(ip)) return true;

        try {
            if(connJson.get("hash") == null) return true;
            String correctHash = connJson.get("hash").getAsString();
            return !correctHash.equals(createHash(hash));
        }
        catch(NoSuchAlgorithmException err) {
            return true;
        }
    }

    public boolean wrongConnection(String ip, String hash) {
        return wrongIp(ip) || !hash.matches("^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)?$") || hash.length() < 30;
    }

    public boolean wrongIp(String Ip) {
        try {
            String correctIp = InetAddress.getByName("smpbot.duckdns.org").getHostAddress();
            return !Ip.equals(correctIp);
        }
        catch(UnknownHostException ignored) {
            return true;
        }
    }

    public String createHash(String originalString) throws NoSuchAlgorithmException {
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
}
