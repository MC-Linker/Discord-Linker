package me.lianecx.discordlinker;

import com.google.common.collect.Lists;
import com.google.gson.*;
import express.Express;
import express.utils.Status;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
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
        config.addDefault("prefix", "&l&9Discord &8| ");
        config.options().copyDefaults(true);
        saveConfig();

        getServer().getPluginManager().registerEvents(new ChatListeners(), this);
        getCommand("linker").setExecutor(new LinkerCommand());
        getCommand("linker").setTabCompleter(new LinkerTabCompleter());

        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            HttpConnection.checkVersion();

            try {
                Reader connReader = Files.newBufferedReader(Paths.get(getDataFolder() + "/connection.conn"));
                JsonElement parser = new JsonParser().parse(connReader);
                connJson = parser.isJsonObject() ? parser.getAsJsonObject() : new JsonObject();

                HttpConnection.send("The server has opened!", "start", null);
            } catch (IOException ignored) {}
        });

        Logger log = (Logger) LogManager.getRootLogger();
        log.addAppender(cmdLogger);

        app = loadExpress();
        getLogger().info(ChatColor.GREEN + "Plugin enabled.");
    }

    @Override
    public void onDisable() {
        HttpConnection.send("The server has shutdown!", "close", null);
        getLogger().info(ChatColor.RED + "Plugin disabled.");
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

        //GET localhost:11111/file/find/?file=level.dat&path=/path/to/file&depth=4
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

            String prefix = ChatColor.translateAlternateColorCodes('&', getConfig().getString("prefix"));
            ComponentBuilder messageBuilder = new ComponentBuilder(prefix)
                    .event(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://top.gg/bot/712759741528408064"))
                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Message sent using \u00A76Minecraft SMP-Bot").create()));

            if(privateMsg) {
                messageBuilder.append(username + " whispers to you: ", ComponentBuilder.FormatRetention.NONE)
                        .italic(true);
            } else {
                messageBuilder.append(username, ComponentBuilder.FormatRetention.NONE)
                    .bold(true)
                    .color(net.md_5.bungee.api.ChatColor.GRAY)

                    .append(" >> ", ComponentBuilder.FormatRetention.NONE)
                    .color(net.md_5.bungee.api.ChatColor.DARK_GRAY);
            }

            //Make links clickable
            Pattern urlPattern = Pattern.compile("((http://|https://)?(\\S*)?(([a-zA-Z0-9-]){2,}\\.){1,4}([a-zA-Z]){2,6}(/([a-zA-Z-_/.0-9#:?=&;,]*)?)?)");
            Matcher matcher = urlPattern.matcher(msg);
            if (matcher.find()) {
                String url = matcher.group();
                List<String> msgArray = Lists.newArrayList(msg.split("\\s+"));

                for (String m : msgArray) {
                    if (m.equals(url)) {
                        messageBuilder.append(m + " ", ComponentBuilder.FormatRetention.NONE)
                                .event(new ClickEvent(ClickEvent.Action.OPEN_URL, m))
                                .underlined(true);

                    } else messageBuilder.append(m + " ", ComponentBuilder.FormatRetention.NONE);

                    //Add italic to the url if it's a private message
                    if(privateMsg) messageBuilder.italic(true);
                }
            } else {
                messageBuilder.append(msg, ComponentBuilder.FormatRetention.NONE);
                if(privateMsg) messageBuilder.italic(true);
            }

            BaseComponent[] messageComponent = messageBuilder.create();

            if(privateMsg) {
                Player player = getServer().getPlayer(targetUsername);
                if(player != null) player.spigot().sendMessage(messageComponent);
                else {
                    res.setStatus(Status._422);
                    res.send("Target player does not exist or is not online.");
                    return;
                }
            } else {
                getServer().spigot().broadcast(messageComponent);
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
            String code = req.getAuthorization().get(0).getData();
            String hash = req.getAuthorization().get(1).getData();

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
                botConnJson.addProperty("version", getServer().getBukkitVersion());
                botConnJson.addProperty("path", URLEncoder.encode(getServer().getWorlds().get(0).getWorldFolder().getCanonicalPath().replaceAll("\\\\", "/"), "utf-8"));

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
                respondJson.addProperty("version", getServer().getBukkitVersion());
                respondJson.addProperty("path", URLEncoder.encode(getServer().getWorlds().get(0).getWorldFolder().getCanonicalPath().replaceAll("\\\\", "/"), "utf-8"));

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
