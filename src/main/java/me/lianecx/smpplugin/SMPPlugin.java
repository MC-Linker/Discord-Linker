package me.lianecx.smpplugin;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import org.bukkit.Material;
import org.bukkit.command.CommandException;
import org.bukkit.configuration.file.FileConfiguration;
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


public final class SMPPlugin extends JavaPlugin {

    private static Express app;
    private static JsonObject connJson;
    private static SMPPlugin plugin;
    private static ConsoleLogger cmdLogger = new ConsoleLogger();
    private static String verifyCode = null;
    FileConfiguration config = getConfig();

    @Override
    public void onEnable() {
        plugin = this;
        config.addDefault("port", 11111);
        config.options().copyDefaults(true);
        saveConfig();

        getServer().getPluginManager().registerEvents(new ChatListeners(), this);

        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            HttpConnection.checkVersion();

            try {
                Reader connReader = Files.newBufferedReader(Paths.get(getDataFolder() + "/connection.conn"));
                JsonElement parser = new JsonParser().parse(connReader);
                connJson = parser.isJsonObject() ? parser.getAsJsonObject() : new JsonObject();

                HttpConnection.send("The server has opened!", 6, null);
            } catch (IOException ignored) {}
        });

        Logger log = (Logger) LogManager.getRootLogger();
        log.addAppender(cmdLogger);

        app = loadExpress();
        getLogger().info(ChatColor.GREEN + "Plugin enabled.");
    }

    @Override
    public void onDisable() {
        HttpConnection.send("The server has shutdown!", 7, null);
        getLogger().info(ChatColor.RED + "Plugin disabled.");
        app.stop();
    }

    public static JsonObject getConnJson() {
        return connJson;
    }
    public static SMPPlugin getPlugin() {
        return plugin;
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
                    res.setStatus(Status._500);
                    res.send("Invalid Path");
                }
            } catch (InvalidPathException | UnsupportedEncodingException err) {
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

            try {
                FileOutputStream outputStream = new FileOutputStream(URLDecoder.decode(req.getQuery("path"), "utf-8"));

                //Transfer body (inputStream) to outputStream
                byte[] buf = new byte[8192];
                int length;
                while ((length = req.getBody().read(buf)) > 0) {
                    outputStream.write(buf, 0, length);
                }

                res.send("Success");
            } catch (InvalidPathException | IOException err) {
                res.setStatus(Status._500);
                res.send(err.toString());
            }
        });

        //GET localhost:11111/file/find/?file=level.dat&path=/path/to/file&depth=4
        app.get("/file/find/", (req, res) -> {
            if(wrongHash(req.getAuthorization().get(0).getData())) {
                res.setStatus(Status._401);
                res.send("Wrong hash");
                return;
            }

            try {
                Path path = Paths.get(URLDecoder.decode(req.getQuery("path"), "utf-8"));

                List<Path> searchFiles = Files.walk(path, Integer.parseInt(req.getQuery("depth")))
                        .filter(Files::isRegularFile)
                        .filter(file -> file.getFileName().toString().equalsIgnoreCase(req.getQuery("file")))
                        .collect(Collectors.toList());
                res.send(searchFiles.get(0).toFile().getCanonicalPath());
            } catch (IOException err) {
                res.setStatus(Status._500);
                res.send(err.toString());
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

            try {
                getServer().getScheduler().runTask(this, () -> {
                    try {
                        cmdLogger.startLogging();
                        getServer().dispatchCommand(Bukkit.getConsoleSender(), URLDecoder.decode(req.getQuery("cmd"), "utf-8"));
                        cmdLogger.stopLogging();
                    } catch(UnsupportedEncodingException err) {
                        res.setStatus(Status._500);
                        res.send(err.toString());
                        return;
                    }

                    try {
                        res.send(ChatColor.stripColor(cmdLogger.getData().get(0)));
                    } catch (IndexOutOfBoundsException err) {
                        res.setStatus(Status._500);
                        res.send("Could not fetch response message! Please restart your server. This commonly happens after using `/reload`");
                    }
                    cmdLogger.clearData();
                });
            } catch (IllegalArgumentException | CommandException err) {
                res.setStatus(Status._500);
                res.send(err.toString());
            }
        });

        //GET localhost:11111/chat/?msg=Ayoo&username=Lianecx
        app.get("/chat/", (req, res) -> {
            if (wrongHash(req.getAuthorization().get(0).getData())) {
                res.setStatus(Status._401);
                res.send("Wrong hash");
                return;
            }

            String msg = req.getQuery("msg");
            String username = req.getQuery("username");
            try {
                msg = URLDecoder.decode(msg, "utf-8");
                username = URLDecoder.decode(username, "utf-8");
            } catch (UnsupportedEncodingException err) {
                res.setStatus(Status._500);
                res.send(err.toString());
                return;
            }

            ComponentBuilder messageBuilder = new ComponentBuilder("Discord")
                    .bold(true)
                    .color(net.md_5.bungee.api.ChatColor.BLUE)
                    .event(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://top.gg/bot/712759741528408064"))
                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Message sent using \u00A76Minecraft SMP-Bot").create()))

                    .append(" | ", ComponentBuilder.FormatRetention.NONE)
                    .color(net.md_5.bungee.api.ChatColor.DARK_GRAY)
                    .bold(true)

                    .append(username, ComponentBuilder.FormatRetention.NONE)
                    .bold(true)
                    .color(net.md_5.bungee.api.ChatColor.GRAY)

                    .append(" >> ", ComponentBuilder.FormatRetention.NONE)
                    .color(net.md_5.bungee.api.ChatColor.DARK_GRAY);

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
                }
            } else messageBuilder.append(msg, ComponentBuilder.FormatRetention.NONE);

            BaseComponent[] messageComponent = messageBuilder.create();
            getServer().spigot().broadcast(messageComponent);
            res.send("Success");
        });

        //GET localhost:11111/disconnect/
        app.get("/disconnect/", (req, res) -> {
            if(wrongHash(req.getAuthorization().get(0).getData())) {
                res.setStatus(Status._400);
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

        /*POST localhost:11111/connect/
            {
                "ip": ip,
                "guild": guildId,
                "channel":channelId,
                "types": [
                      {
                        "type": 0,
                        "enabled": true
                      },
                      ...
                 ]
            }
         */
        app.post("/channel/", (req, res) -> {
            JsonObject parser = new JsonParser().parse(new InputStreamReader(req.getBody())).getAsJsonObject();
            String hash = req.getAuthorization().get(0).getData();

            if(wrongHash(hash)) {
                res.setStatus(Status._401);
                res.send("Wrong hash");
                return;
            }

            try {
                connJson = new JsonObject();
                connJson.addProperty("hash", createHash(hash));
                connJson.addProperty("chat", true);
                connJson.add("guild", parser.get("guild"));
                connJson.add("ip", parser.get("ip"));
                connJson.add("types", parser.get("types").getAsJsonArray());
                connJson.add("channel", parser.get("channel"));
                updateConn();

                JsonObject botConnJson = new JsonObject();
                botConnJson.addProperty("chat", true);
                botConnJson.add("guild", parser.get("guild"));
                botConnJson.add("ip", parser.get("ip"));
                botConnJson.add("types", parser.get("types").getAsJsonArray());
                botConnJson.add("channel", parser.get("channel"));
                botConnJson.addProperty("hash", hash);
                botConnJson.addProperty("version", getServer().getBukkitVersion());
                botConnJson.addProperty("path", URLEncoder.encode(getServer().getWorlds().get(0).getWorldFolder().getCanonicalPath().replaceAll("\\\\", "/"), "utf-8"));

                res.send(botConnJson.toString());
            } catch (IOException | NoSuchAlgorithmException err) {
                res.setStatus(Status._500);
                res.send(err.toString());
            }
        });

        //GET localhost:11111/
        app.get("/", (req, res) -> res.send("To invite the Minecraft SMP Bot, open this link: https://top.gg/bot/712759741528408064"));

        int port = config.getInt("port") != 0 ? config.getInt("port") : 11111;
        app.listen(() -> getLogger().info("Listening on port " + port), port);

        return app;
    }

    private void updateConn() throws IOException {
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
        } catch (UnknownHostException e) {
            return true;
        }
    }

    public String createHash(String originalString) throws NoSuchAlgorithmException {
        final MessageDigest digest = MessageDigest.getInstance("SHA3-256");
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
