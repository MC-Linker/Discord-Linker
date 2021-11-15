package me.lianecx.smpbotplugin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import express.http.request.Request;
import express.utils.Status;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandException;
import org.bukkit.plugin.java.JavaPlugin;
import express.Express;

import java.io.*;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class SMPBotPlugin extends JavaPlugin {
    public Express app;

    @Override
    public void onEnable() {
        getConfig().options().copyDefaults();
        saveDefaultConfig();
        app = loadExpress();
        getServer().getPluginManager().registerEvents(new ChatListeners(), this);

        ChatListeners.send("The server has opened!", 6, null);
        getLogger().info(ChatColor.GREEN + "[SMPBot] Plugin enabled.");
    }

    @Override
    public void onDisable() {
        app.stop();
        ChatListeners.send("The server has shutdown!", 7, null);
        getLogger().info(ChatColor.RED + "[SMPBot] Plugin disabled.");
    }

    public Express loadExpress() {
        Express app = new Express();

        //GET localhost:21000/file/get/?hash=hash&path=/path/to/file
        app.get("/file/get/", (req, res) -> {
            if(checkHash(req.getQuery("hash"))) {
                try {
                    Path file = Paths.get(req.getQuery("path"));
                    if(res.sendAttachment(file)) {}
                    else {
                        res.setStatus(Status._500);
                        res.send("Invalid Path");
                    }
                } catch (InvalidPathException err) {
                    res.setStatus(Status._500);
                    res.send(err.toString());
                }
            } else {
                res.setStatus(Status._400);
                res.send("Wrong hash");
            }
        });

        /*POST localhost:21000/file/put/?hash=hash
            { fileStreamtToFile }
         */
        app.post("/file/put/", (req, res) -> {
            if(checkHash(req.getQuery("hash"))) {
                try {
                    FileOutputStream outputStream = new FileOutputStream(req.getQuery("path"));
                    req.getBody().transferTo(outputStream);
                    res.send("Success");
                } catch (InvalidPathException | FileNotFoundException err) {
                    res.setStatus(Status._500);
                    res.send(err.toString());
                } catch(IOException err) {
                    res.setStatus(Status._500);
                    res.send(err.toString());
                }
            } else {
                res.setStatus(Status._400);
                res.send("Wrong hash");
            }
        });

        //GET localhost:21000/file/find/?hash=hash&file=level.dat&path=/path/to/file&depth=4
        app.get("/file/find/", (req, res) -> {
            if(checkHash(req.getQuery("hash"))) {
                try {
                    List<Path> searchFiles = Files.walk(Paths.get(req.getQuery("path")), Integer.parseInt(req.getQuery("depth")))
                            .filter(Files::isRegularFile)
                            .filter(file -> file.getFileName().toString().equalsIgnoreCase(req.getQuery("file")))
                            .collect(Collectors.toList());
                    res.send(searchFiles.get(0).toFile().getCanonicalPath());
                } catch (IOException err) {
                    res.setStatus(Status._500);
                    res.send(err.toString());
                }
            } else {
                res.setStatus(Status._400);
                res.send("Wrong hash");
            }
        });

        //GET localhost:21000/command/?hash=hash&cmd=ban+Lianecx
        app.get("/command/", (req, res) -> {
            if(checkHash(req.getQuery("hash"))) {
                try {
                    Bukkit.getScheduler().runTask(this, () ->
                            getServer().dispatchCommand(Bukkit.getConsoleSender(), URLDecoder.decode(req.getQuery("cmd"), StandardCharsets.UTF_8)));
                    res.send("Success");
                } catch(CommandException | IllegalArgumentException err) {
                    res.setStatus(Status._500);
                    res.send(err.toString());
                }
            } else {
                res.setStatus(Status._400);
                res.send("Wrong hash");
            }
        });

        //GET localhost:21000/chat/?hash=hash"&msg=bruhhhhh+bruh&username=Lianecx
        app.get("/chat/", (req, res) -> {
            if(checkHash(req.getQuery("hash"))) {
                String msg = req.getQuery("msg");
                String username = req.getQuery("username");
                msg = URLDecoder.decode(msg, StandardCharsets.UTF_8);
                username = URLDecoder.decode(username, StandardCharsets.UTF_8);

                ComponentBuilder messageBuilder = new ComponentBuilder("Discord")
                    .bold(true)
                    .color(net.md_5.bungee.api.ChatColor.BLUE)
                    .event(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://top.gg/bot/712759741528408064"))
                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Message sent using \u00A76SMP-Bot")))
                    .append(" | " + username, ComponentBuilder.FormatRetention.NONE)
                        .bold(true)
                    .append(" >> ", ComponentBuilder.FormatRetention.NONE)
                        .color(net.md_5.bungee.api.ChatColor.DARK_GRAY);

                Pattern urlPattern = Pattern.compile("((http://|https://)?(\\S*)?(([a-zA-Z0-9-]){2,}\\.){1,4}([a-zA-Z]){2,6}(/([a-zA-Z-_/.0-9#:?=&;,]*)?)?)");
                Matcher matcher = urlPattern.matcher(msg);
                if (matcher.find()) {
                    String url = matcher.group();
                    List<String> msgArray = Arrays.stream(msg.split("\\s+")).toList();

                    for (String m : msgArray) {
                        if(m.equals(url)) {
                            if(!m.startsWith("https://") && !m.startsWith("http://")) m = "https://" + m;
                            messageBuilder.append(m + " ", ComponentBuilder.FormatRetention.NONE)
                                .event(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
                                .underlined(true);
                        } else messageBuilder.append(m + " ", ComponentBuilder.FormatRetention.NONE);
                    }
                } else {
                    messageBuilder.append(msg, ComponentBuilder.FormatRetention.NONE);
                }

                BaseComponent[] messageComponent = messageBuilder.create();
                getServer().spigot().broadcast(messageComponent);
                res.send("Success");
            } else {
                res.setStatus(Status._400);
                res.send("Wrong hash");
            }
        });

        /*POST localhost:21000/connect/
            {
                "hash": hash,
                "chat": true,
                "guild": guildId,
                ["channel":channelId,
                "types": [
                      {
                        "type": 0,
                        "enabled": true
                      },
                      ...
                 ]]
            }
         */
        app.post("/connect/", (req, res) -> {
            getLogger().info("Connection request...");
            JsonObject parser = new JsonParser().parse(new InputStreamReader(req.getBody())).getAsJsonObject();
            if(checkConnection(req, parser.get("hash").getAsString())) {
                try {
                    JsonObject connectJson = new JsonObject();
                    connectJson.add("hash", parser.get("hash"));
                    connectJson.add("chat", parser.get("chat"));
                    connectJson.add("guild", parser.get("guild"));
                    if(parser.get("chat").getAsBoolean()) {
                        connectJson.add("types", parser.get("types").getAsJsonArray());
                        connectJson.addProperty("channel", parser.get("channel").getAsString());
                    }

                    FileWriter writer = new FileWriter(getServer().getPluginManager().getPlugin("SMPBotPlugin").getDataFolder() + "/connection.conn");
                    writer.write(connectJson.toString());
                    writer.close();

                    connectJson.addProperty("ip", InetAddress.getLocalHost().getHostAddress());
                    connectJson.addProperty("version", getServer().getBukkitVersion());
                    connectJson.addProperty("path", getServer().getWorldContainer().getCanonicalPath().replaceAll("\\\\", "/") + "/" + getServer().getWorlds().get(0).getName());

                    res.send(connectJson.toString());
                    getLogger().info("Successfully connected with guildId: " + parser.get("guild"));
                } catch (IOException err) {
                    res.send(err.toString());
                    res.setStatus(Status._500);
                    getLogger().info("Cannot save hash");
                }
            } else {
                getLogger().info("Connection unsuccessful");
                res.setStatus(Status._400);
                res.send("Wrong hash-format or IP");
            }
        });

        //GET localhost:21000/
        app.get("/", (req, res) -> {
            res.send("To invite the bot, click this link: https://top.gg/bot/712759741528408064");
        });

        int port = 21000;
        app.listen(() -> getLogger().info("Listening on port " + port), port);
        return app;
    }

    public boolean checkHash(String hash) {
        try {
            Reader reader = Files.newBufferedReader(Paths.get(getServer().getPluginManager().getPlugin("SMPBotPlugin").getDataFolder() + "/connection.conn"));
            JsonObject parser = new JsonParser().parse(reader).getAsJsonObject();
            String correctHash = URLDecoder.decode(parser.get("hash").getAsString(), StandardCharsets.UTF_8);
            reader.close();
            return correctHash.equals(hash);
        } catch(IOException | JsonParseException err) {
            return false;
        }
    }

    public boolean checkConnection(Request req, String hash) {
        //TODO add IP check
        return URLDecoder.decode(hash, StandardCharsets.UTF_8).matches("^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)?$");
    }
}
