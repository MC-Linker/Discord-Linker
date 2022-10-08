package me.lianecx.discordlinker;

import com.google.gson.*;
import express.Express;
import express.utils.Status;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


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
        config.addDefault("port", 11111);
        config.addDefault("message", "&l&9Discord &8| &l&7%username% &8>>&r %message%");
        config.addDefault("private_message", "&l&9Discord &8| &o&7%username% whispers to you: %message%");
        config.options().copyDefaults(true);
        saveConfig();

        getServer().getPluginManager().registerEvents(new ChatListeners(), this);
        getCommand("linker").setExecutor(new LinkerCommand());
        getCommand("linker").setTabCompleter(new LinkerTabCompleter());

        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            HttpConnection.checkVersion();

            try (Reader connReader = Files.newBufferedReader(Paths.get(getDataFolder() + "/connection.conn"))) {
                JsonElement parser = new JsonParser().parse(connReader);
                connJson = parser.isJsonObject() ? parser.getAsJsonObject() : new JsonObject();

                HttpConnection.send("", "start", null);
            } catch (IOException ignored) {}
        });

        Logger log = (Logger) LogManager.getRootLogger();
        log.addAppender(cmdLogger);

        app = loadExpress();
        getLogger().info(ChatColor.GREEN + "Plugin enabled.");
    }

    @Override
    public void onDisable() {
        HttpConnection.send("", "close", null);
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

    public Express loadExpress() {
        Express app = new Express();

        //GET localhost:11111/file/get/?path=/path/to/file
        app.get("/file/get/", (req, res) -> {
            if(wrongHash(req.getAuthorization().get(0).getData())) {
                res.setStatus(Status._401);
                res.send("Wrong hash");
                return;
            }

            try {
                Path file = Paths.get(URLDecoder.decode(req.getQuery("path"), "utf-8"));

                if(!res.sendAttachment(file)) {
                    res.setStatus(Status._404);
                    res.send("Invalid Path");
                }
            } catch (InvalidPathException err) {
                res.setStatus(Status._404);
                res.send("Invalid Path");
            } catch (UnsupportedEncodingException err) {
                res.setStatus(Status._500);
                res.send(err.toString());
            }
        });

        /*POST localhost:11111/file/put/
            { fileStreamToFile }
         */
        app.post("/file/put/", (req, res) -> {
            if(wrongHash(req.getAuthorization().get(0).getData())) {
                res.setStatus(Status._401);
                res.send("Wrong hash");
                return;
            }

            try (FileOutputStream outputStream = new FileOutputStream(URLDecoder.decode(req.getQuery("path"), "utf-8"))) {

                //Transfer body (inputStream) to outputStream
                byte[] buf = new byte[8192];
                int length;
                while ((length = req.getBody().read(buf)) > 0) {
                    outputStream.write(buf, 0, length);
                }

                res.send("Success");
            } catch (InvalidPathException err) {
                res.setStatus(Status._404);
                res.send("Invalid Path");
            } catch(IOException err) {
                res.setStatus(Status._500);
                res.send(err.toString());
            }
        });

        //GET localhost:11111/file/list/?folder="/world/"
        app.get("/file/list/", (req, res) -> {
            if(wrongHash(req.getAuthorization().get(0).getData())) {
                res.setStatus(Status._401);
                res.send("Wrong hash");
                return;
            }

            try {
                Path folder = Paths.get(URLDecoder.decode(req.getQuery("folder"), "utf-8"));

                JsonArray content = new JsonArray();
                Files.list(folder)
                        .map(path -> {
                            JsonObject object = new JsonObject();
                            object.addProperty("name", path.toFile().getName());
                            object.addProperty("isDirectory", path.toFile().isDirectory());
                            return object;
                        })
                        .forEach(content::add);

                res.send(content.toString());
            } catch (InvalidPathException err) {
                res.setStatus(Status._404);
                res.send("Invalid Path");
            } catch (IOException err) {
                res.send("[]");
            }
        });

        //GET localhost:11111/verify/
        app.get("/verify/", (req, res) -> {
            verifyCode = RandomStringUtils.randomAlphanumeric(6);
            getLogger().info("Verification Code: " + verifyCode);

            getServer().getScheduler().runTaskLater(this, () -> verifyCode = null, 3600);

            res.send("Success");
        });

        //GET localhost:11111/command/?cmd=ban+Lianecx
        app.get("/command/", (req, res) -> {
            if (wrongHash(req.getAuthorization().get(0).getData())) {
                res.setStatus(Status._401);
                res.send("Wrong hash");
                return;
            }

            JsonObject responseJson = new JsonObject();

            try {
                getServer().getScheduler().runTask(this, () -> {
                    try {
                        cmdLogger.startLogging();
                        getServer().dispatchCommand(Bukkit.getConsoleSender(), URLDecoder.decode(req.getQuery("cmd"), "utf-8"));
                    } catch(UnsupportedEncodingException err) {
                        responseJson.addProperty("message", err.toString());
                        res.setStatus(Status._500);
                        res.send(responseJson.toString());
                        return;
                    } finally {
                        cmdLogger.stopLogging();
                    }

                    try {
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
                    } catch (IndexOutOfBoundsException err) {
                        res.setStatus(Status._206);
                        responseJson.addProperty("message", err.toString());

                        res.send(responseJson.toString());
                    } finally {
                        cmdLogger.clearData();
                    }
                });
            } catch (IllegalArgumentException | CommandException err) {
                res.setStatus(Status._500);
                responseJson.addProperty("message", err.toString());
                res.send(responseJson.toString());
            }
        });

        /*POST localhost:11111/chat/
            {
                "msg": "Ayoo",
                "username": "Lianecx,
                "private": false
            }
         */
        app.post("/chat/", (req, res) -> {
            if (wrongHash(req.getAuthorization().get(0).getData())) {
                res.setStatus(Status._401);
                res.send("Wrong hash");
                return;
            }

            JsonObject parser = new JsonParser().parse(new InputStreamReader(req.getBody())).getAsJsonObject();

            String msg;
            String username;
            boolean privateMsg;
            String targetUsername = "";
            try {
                msg = parser.get("msg").getAsString();
                username = parser.get("username").getAsString();
                privateMsg = parser.get("private").getAsBoolean();
                if(privateMsg) targetUsername = parser.get("target").getAsString();
            } catch(ClassCastException err) {
                res.setStatus(Status._400);
                res.send("Invalid JSON");
                return;
            }

            //Format **bold**
            msg = msg.replaceAll("\\*\\*(.+?)\\*\\*", "&l$1&r");
            //Format __underline__
            msg = msg.replaceAll("__(.+?)__", "&n$1&r");
            //Format *italic* and _italic_
            msg = msg.replaceAll("_(.+?)_|\\*(.+?)\\*", "&o$1$2&r");
            //Format ~~strikethrough~~
            msg = msg.replaceAll("~~(.+?)~~", "&m$1&r");
            //Format ??obfuscated??
            msg = msg.replaceAll("\\?\\?(.+?)\\?\\?", "&k$1&r");
            //Format inline and multiline `code` blocks
            msg = msg.replaceAll("(?s)```[^\\n]*\\n(.+)```|```(.+)```", "&7&n$1$2&r");
            msg = msg.replaceAll("`(.+?)`", "&7&n$1&r");
            //Format ||spoilers||
            msg = msg.replaceAll("\\|\\|(.+?)\\|\\|", "&8$1&r");
            //Format '> quotes'
            msg = msg.replaceAll(">+ (.+)", "&7| $1&r");

            //Get config string and replace placeholders
            String chatMessage = getConfig().getString(privateMsg ? "private_message" : "message");
            chatMessage = chatMessage.replaceAll("%message%", msg);
            chatMessage = chatMessage.replaceAll("%username%", username);

            //Translate color codes
            chatMessage = ChatColor.translateAlternateColorCodes('&', chatMessage);

            //Make links clickable
            String urlRegex = "https?://[-\\w_.]{2,}\\.[a-z]{2,4}/\\S*?";
            String mdUrlRegex = "(?i)\\[([^]]+)]\\((" + urlRegex + ")\\)";
            Pattern mdUrlPattern = Pattern.compile(mdUrlRegex);

            ComponentBuilder chatBuilder = new ComponentBuilder("");

            StringBuilder tempMessage = new StringBuilder();
            for(String word : chatMessage.split(" ")) {
                if(word.matches(urlRegex)) {
                    if(tempMessage.length() != 0) chatBuilder.append(tempMessage.toString(), ComponentBuilder.FormatRetention.NONE);
                    tempMessage.setLength(0); //Clear tempMessage

                    chatBuilder.append(word);
                    chatBuilder.event(new ClickEvent(ClickEvent.Action.OPEN_URL, word));
                    chatBuilder.underlined(true);
                    tempMessage.append(" ");
                } else if(word.matches(mdUrlRegex)) {
                    if(tempMessage.length() != 0) chatBuilder.append(tempMessage.toString(), ComponentBuilder.FormatRetention.NONE);
                    tempMessage.setLength(0); //Clear tempMessage

                    Matcher matcher = mdUrlPattern.matcher(word);
                    matcher.find();

                    chatBuilder.append(matcher.group(1));
                    chatBuilder.event(new ClickEvent(ClickEvent.Action.OPEN_URL, matcher.group(2)));
                    chatBuilder.underlined(true);
                    tempMessage.append(" ");
                } else {
                    tempMessage.append(word).append(" ");
                }
            }
            if(tempMessage.length() != 0) chatBuilder.append(tempMessage.toString(), ComponentBuilder.FormatRetention.NONE);

            if(privateMsg) {
                Player player = getServer().getPlayer(targetUsername);
                if(player == null) {
                    res.setStatus(Status._422);
                    res.send("Target player does not exist or is not online.");
                    return;
                }

                player.spigot().sendMessage(chatBuilder.create());
            } else {
                getServer().spigot().broadcast(chatBuilder.create());
            }

            res.send("Success");
        });

        //GET localhost:11111/disconnect/
        app.get("/disconnect/", (req, res) -> {
            if(wrongHash(req.getAuthorization().get(0).getData())) {
                res.setStatus(Status._401);
                res.send("Wrong hash");
                return;
            }

            connJson = new JsonObject();
            File connection = new File(getDataFolder() + "/connection.conn");
            boolean deleted = connection.delete();
            if(deleted) {
                getLogger().info("Disconnected from discord...");
                res.send("Success");
            } else {
                res.setStatus(Status._500);
                res.send("Could not delete file");
            }
        });

        /*POST localhost:11111/connect/
            {
                "ip": ip,
                "guild": guildId
            }
         */
        app.post("/connect/", (req, res) -> {
            getLogger().info("Connection request...");
            JsonObject parser = new JsonParser().parse(new InputStreamReader(req.getBody())).getAsJsonObject();
            String hash = req.getAuthorization().get(0).getData();
            String code = req.getAuthorization().get(1).getData();

            if(wrongConnection(req.getIp(), hash)) {
                getLogger().info("Connection unsuccessful");
                res.setStatus(Status._400);
                res.send("Wrong hash-format or IP");
                return;
            } else if(!req.hasAuthorization() || !code.equals(verifyCode)) {
                getLogger().info("Connection unsuccessful");
                res.setStatus(Status._401);
                res.send("Wrong authorization");
                verifyCode = null;
                return;
            }

            try {
                connJson = new JsonObject();
                connJson.addProperty("hash", createHash(hash));
                connJson.addProperty("chat", false);
                connJson.add("guild", parser.get("guild"));
                connJson.add("ip", parser.get("ip"));
                updateConn();

                JsonObject botConnJson = new JsonObject();
                botConnJson.addProperty("chat", false);
                botConnJson.add("guild", parser.get("guild"));
                botConnJson.add("ip", parser.get("ip"));
                botConnJson.addProperty("hash", hash);
                botConnJson.addProperty("version", Bukkit.getBukkitVersion().split("-")[0]);
                botConnJson.addProperty("online", getServer().getOnlineMode());
                botConnJson.addProperty("path", getWorldPath());

                res.send(botConnJson.toString());
                getLogger().info("Successfully connected with discord server. Id: " + parser.get("guild"));
            } catch (IOException | NoSuchAlgorithmException err) {
                getLogger().info("Connection unsuccessful");
                res.setStatus(Status._500);
                res.send(err.toString());
            } finally {
                verifyCode = null;
            }
        });

        /*POST localhost:11111/channel/
            {
                "ip": ip,
                "guild": guildId,
                "channel": {
                    channelId,
                    "types": ["chat", "close"]
                }
            }
         */
        app.post("/channel/:method/", (req, res) -> {
            String hash = req.getAuthorization().get(0).getData();

            if(wrongHash(hash)) {
                res.setStatus(Status._401);
                res.send("Wrong hash");
                return;
            }

            JsonObject parser = new JsonParser().parse(new InputStreamReader(req.getBody())).getAsJsonObject();

            try {
                //Save channels from connJson and add new channel
                JsonArray oldChannels = new JsonArray();
                if(connJson != null && connJson.get("channels") != null) oldChannels = connJson.getAsJsonArray("channels");

                JsonArray channels = new JsonArray();
                //Remove duplicates and fill new channel array
                for (JsonElement channel : oldChannels) {
                    if(!channel.getAsJsonObject().get("id").equals(parser.getAsJsonObject("channel").get("id"))) {
                        channels.add(channel);
                    }
                }

                //Add new channel if method equals to add
                if(req.getParam("method").equals("add")) channels.add(parser.get("channel"));
                else if(!req.getParam("method").equals("remove")){
                    //If neither add nor remove
                    res.setStatus(Status._400);
                    res.send("Invalid method parameter");
                    return;
                }

                //Create new connJson
                connJson = new JsonObject();
                connJson.addProperty("hash", createHash(hash));
                connJson.addProperty("chat", channels.size() != 0);
                connJson.add("guild", parser.get("guild"));
                connJson.add("ip", parser.get("ip"));
                connJson.add("channels", channels);
                updateConn();

                JsonObject respondJson = new JsonObject();
                respondJson.addProperty("chat", true);
                respondJson.add("guild", parser.get("guild"));
                respondJson.add("ip", parser.get("ip"));
                respondJson.add("channels", channels);
                respondJson.addProperty("hash", hash);
                respondJson.addProperty("online", getServer().getOnlineMode());
                respondJson.addProperty("version", Bukkit.getVersion().split("-")[0]);
                respondJson.addProperty("path", getWorldPath());

                res.send(respondJson.toString());
            } catch (IOException | NoSuchAlgorithmException err) {
                res.setStatus(Status._500);
                res.send(err.toString());
            }
        });

        app.get("/players/", (req, res) -> {
            if(wrongHash(req.getAuthorization().get(0).getData())) {
                res.setStatus(Status._401);
                res.send("Wrong hash");
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

    public String getWorldPath() throws IOException {
        Properties serverProperties = new Properties();
        serverProperties.load(Files.newInputStream(Paths.get("server.properties")));
        String worldName = serverProperties.getProperty("level-name");

        String path = getServer().getWorldContainer().getCanonicalPath() + "/" + worldName;
        return URLEncoder.encode(path, "utf-8");
    }

    public void updateConn() throws IOException {
        FileWriter writer = new FileWriter(getDataFolder() + "/connection.conn");
        writer.write(connJson.toString());
        writer.close();
    }

    public boolean wrongHash(String hash) {
        try {
            if(connJson.get("hash") == null) return true;
            String correctHash = connJson.get("hash").getAsString();
            return !correctHash.equals(createHash(hash));
        } catch(NoSuchAlgorithmException err) {
            return true;
        }
    }

    public boolean wrongConnection(String Ip, String hash) {
        try {
            String correctIp = InetAddress.getByName("smpbot.duckdns.org").getHostAddress();
            return !hash.matches("^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)?$") || hash.length()<30 || !Ip.equals(correctIp);
        } catch (UnknownHostException ignored) {
            return true;
        }
    }

    public String createHash(String originalString) throws NoSuchAlgorithmException {
        final MessageDigest digest = MessageDigest.getInstance("SHA-256");
        final byte[] hashBytes = digest.digest(originalString.getBytes(StandardCharsets.UTF_8));

        StringBuilder hexString = new StringBuilder(2 * hashBytes.length);
        for (byte hashByte : hashBytes) {
            String hex = Integer.toHexString(0xff & hashByte);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }

        return hexString.toString();
    }
}
