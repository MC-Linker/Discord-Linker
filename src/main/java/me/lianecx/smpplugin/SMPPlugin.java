package me.lianecx.smpplugin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import express.Express;
import express.http.request.Request;
import express.utils.Status;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

        //GET localhost:11111/file/get/?hash=hash&path=/path/to/file
        app.get("/file/get/", (req, res) -> {
            if(wrongHash(req.getQuery("hash"))) {
                res.setStatus(Status._400);
                res.send("Wrong hash");
                return;
            }

            try {
                Path file = Paths.get(URLDecoder.decode(req.getQuery("path"), StandardCharsets.UTF_8));

                if(!res.sendAttachment(file)) {
                    res.setStatus(Status._500);
                    res.send("Invalid Path");
                }
            } catch (InvalidPathException err) {
                res.setStatus(Status._500);
                res.send(err.toString());
            }
        });

        /*POST localhost:11111/file/put/?hash=hash
            { fileStreamToFile }
         */
        app.post("/file/put/", (req, res) -> {
            if(wrongHash(req.getQuery("hash"))) {
                res.setStatus(Status._400);
                res.send("Wrong hash");
                return;
            }

            try {
                FileOutputStream outputStream = new FileOutputStream(URLDecoder.decode(req.getQuery("path"), StandardCharsets.UTF_8));
                req.getBody().transferTo(outputStream);
                res.send("Success");
            } catch (InvalidPathException | IOException err) {
                res.setStatus(Status._500);
                res.send(err.toString());
            }
        });

        //GET localhost:11111/file/find/?hash=hash&file=level.dat&path=/path/to/file&depth=4
        app.get("/file/find/", (req, res) -> {
            if(wrongHash(req.getQuery("hash"))) {
                res.setStatus(Status._400);
                res.send("Wrong hash");
                return;
            }

            try {
                Path path = Paths.get(URLDecoder.decode(req.getQuery("path"), StandardCharsets.UTF_8));

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

        /*POST localhost:11111/log/
            {
                "message": "some message"
            }
         */
        app.post("/log/", (req, res) -> {
            JsonObject parser = new JsonParser().parse(new InputStreamReader(req.getBody())).getAsJsonObject();
            if(wrongConnection(req, parser.get("hash").getAsString())) {
                res.setStatus(Status._400);
                res.send("Wrong hash-format or IP");
                return;
            }
            getLogger().info(parser.get("message").getAsString());
            res.send("Success");
        });

        //GET localhost:11111/command/?hash=hash&cmd=ban+Lianecx
        app.get("/command/", (req, res) -> {
            if(wrongHash(req.getQuery("hash"))) {
                res.setStatus(Status._400);
                res.send("Wrong hash");
                return;
            }

            try {
                getServer().getScheduler().runTask(this, () ->
                        getServer().dispatchCommand(Bukkit.getConsoleSender(), URLDecoder.decode(req.getQuery("cmd"), StandardCharsets.UTF_8)));
                res.send("Success");
            } catch(CommandException | IllegalArgumentException err) {
                res.setStatus(Status._500);
                res.send(err.toString());
            }
        });

        //GET localhost:11111/chat/?hash=hash"&msg=bruhhhhh&username=Lianecx
        app.get("/chat/", (req, res) -> {
            if(wrongHash(req.getQuery("hash"))) {
                res.setStatus(Status._400);
                res.send("Wrong hash");
                return;
            }

            String msg = req.getQuery("msg");
            String username = req.getQuery("username");
            msg = URLDecoder.decode(msg, StandardCharsets.UTF_8);
            username = URLDecoder.decode(username, StandardCharsets.UTF_8);

            ComponentBuilder messageBuilder = new ComponentBuilder("Discord")
                .bold(true)
                .color(net.md_5.bungee.api.ChatColor.BLUE)
                .event(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://top.gg/bot/712759741528408064"))
                .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Message sent using \u00A76Minecraft SMP-Bot").create()))
                .append(" | " + username, ComponentBuilder.FormatRetention.NONE)
                    .bold(true)
                .append(" >> ", ComponentBuilder.FormatRetention.NONE)
                    .color(net.md_5.bungee.api.ChatColor.DARK_GRAY);

            Pattern urlPattern = Pattern.compile("((http://|https://)?(\\S*)?(([a-zA-Z0-9-]){2,}\\.){1,4}([a-zA-Z]){2,6}(/([a-zA-Z-_/.0-9#:?=&;,]*)?)?)");
            Matcher matcher = urlPattern.matcher(msg);
            if (matcher.find()) {
                String url = matcher.group();
                List<String> msgArray = List.of(msg.split("\\s+"));

                for (String m : msgArray) {
                    if(m.equals(url)) {
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

        //GET localhost:11111/disconnect/?hash=hash
        app.get("/disconnect/", (req, res) -> {
            if(wrongHash(req.getQuery("hash"))) {
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
                "hash": hash,
                "chat": true,
                "guild": guildId,
                ["channel"]:channelId,
                ["types"]: [
                      {
                        "type": 0,
                        "enabled": true
                      },
                      ...
                 ]
            }
         */
        app.post("/connect/", (req, res) -> {
            getLogger().info("Connection request...");
            JsonObject parser = new JsonParser().parse(new InputStreamReader(req.getBody())).getAsJsonObject();
            if(wrongConnection(req, parser.get("hash").getAsString())) {
                getLogger().info("Connection unsuccessful");
                res.setStatus(Status._400);
                res.send("Wrong hash-format or IP");
                return;
            }

            try {
                connJson = new JsonObject();
                connJson.addProperty("hash", createHash(parser.get("hash").getAsString()));
                connJson.add("chat", parser.get("chat"));
                connJson.add("guild", parser.get("guild"));
                connJson.add("ip", parser.get("ip"));
                if(parser.get("chat").getAsBoolean()) {
                    connJson.add("types", parser.get("types").getAsJsonArray());
                    connJson.add("channel", parser.get("channel"));
                }

                FileWriter writer = new FileWriter(getDataFolder() + "/connection.conn");
                writer.write(connJson.toString());
                writer.close();

                JsonObject botConnJson = new JsonObject();
                botConnJson.add("chat", parser.get("chat"));
                botConnJson.add("guild", parser.get("guild"));
                botConnJson.add("ip", parser.get("ip"));
                if(parser.get("chat").getAsBoolean()) {
                    botConnJson.add("types", parser.get("types").getAsJsonArray());
                    botConnJson.add("channel", parser.get("channel"));
                }
                botConnJson.add("hash", parser.get("hash"));
                botConnJson.addProperty("version", getServer().getBukkitVersion());
                botConnJson.addProperty("path", URLEncoder.encode(getServer().getWorlds().get(0).getWorldFolder().getCanonicalPath().replaceAll("\\\\", "/"), StandardCharsets.UTF_8));

                res.send(botConnJson.toString());
                getLogger().info("Successfully connected with discord server. Id: " + parser.get("guild"));
            } catch (IOException | NoSuchAlgorithmException err) {
                getLogger().info("Connection unsuccessful");
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

    public boolean wrongHash(String hash) {
        try {
            if(connJson.get("hash") == null) return true;
            String correctHash = connJson.get("hash").getAsString();

            hash = URLEncoder.encode(hash, StandardCharsets.UTF_8);
            return !correctHash.equals(createHash(hash));
        } catch(NoSuchAlgorithmException err) {
            return true;
        }
    }       

    public boolean wrongConnection(Request req, String hash) {
        try {
            String correctAddress = InetAddress.getByName("smpbot.duckdns.org").getHostAddress();
            hash = URLDecoder.decode(hash, StandardCharsets.UTF_8);
            return !hash.matches("^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)?$") || hash.length()<30 /*|| !req.getIp().equals(correctAddress)*/;
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
