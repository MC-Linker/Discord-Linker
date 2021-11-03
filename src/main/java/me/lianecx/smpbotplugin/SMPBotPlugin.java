package me.lianecx.smpbotplugin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonStreamParser;
import express.http.request.Request;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandException;
import org.bukkit.plugin.java.JavaPlugin;
import express.Express;

import java.io.*;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public final class SMPBotPlugin extends JavaPlugin {
    public Express app;

    @Override
    public void onEnable() {
        getConfig().options().copyDefaults();
        saveDefaultConfig();
        app = loadExpress();
        getServer().getPluginManager().registerEvents(new ChatListeners(), this);

        ChatListeners.send("Startup", 7, null);
        getLogger().info(ChatColor.GREEN + "[SMPBot] Plugin enabled.");
    }

    @Override
    public void onDisable() {
        app.stop();
        ChatListeners.send("Shutdown", 8, null);
        getLogger().info(ChatColor.RED + "[SMPBot] Plugin disabled.");
    }

    public Express loadExpress() {
        Express app = new Express();

        //TODO 192.168.178.26:3000/file/get/?hash=test&path=.console_history
        app.get("/file/get/", (req, res) -> {
            getLogger().info("Download request...");
            boolean checkHash = checkHash(req.getQuery("hash"));
            if(checkHash) {
                try {
                    Path file = Paths.get(req.getQuery("path"));
                    getLogger().info("Sending file: " + file.getFileName());
                    boolean sendFile = res.sendAttachment(file);
                    if(sendFile) getLogger().info("Success");
                    else {
                        res.send("Invalid Path");
                        getLogger().info("Invalid Path");
                    }
                } catch (InvalidPathException err) {
                    res.send(err.toString());
                    getLogger().info("Invalid Path");
                }
            } else {
                getLogger().info("Download unsuccessful");
                res.send("Wrong hash");
            }
        });

        //TODO 192.168.178.26:3000/file/put/?hash=test&path=put.txt
        app.post("/file/put/", (req, res) -> {
            getLogger().info("Upload request...");
            boolean checkHash = checkHash(req.getQuery("hash"));
            if(checkHash) {
                try {
                    FileOutputStream outputStream = new FileOutputStream(req.getQuery("path"));
                    req.getBody().transferTo(outputStream);
                    res.send("Success");
                    getLogger().info("Success");
                } catch (InvalidPathException | FileNotFoundException err) {
                    res.send(err.toString());
                    getLogger().info("Invalid Path");
                } catch(IOException err) {
                    res.send(err.toString());
                    getLogger().info("Cannot write to file");
                }
            } else {
                getLogger().info("Upload unsuccessful");
                res.send("Wrong hash");
            }
        });

        //TODO 192.168.178.26:3000/file/find/?hash=test&file=level.dat&path=D:\emili\Documents\SMPBot\Server&depth=4
        app.get("/file/find/", (req, res) -> {
            getLogger().info("Find file request...");
            boolean checkHash = checkHash(req.getQuery("hash"));
            if(checkHash) {
                try {
                    List<Path> searchFiles = Files.walk(Paths.get(req.getQuery("path")), Integer.parseInt(req.getQuery("depth")))
                            .filter(Files::isRegularFile)
                            .filter(file -> file.getFileName().toString().equalsIgnoreCase(req.getQuery("file")))
                            .collect(Collectors.toList());
                    res.send(searchFiles.get(0).toFile().getCanonicalPath());
                    getLogger().info("Found file");
                } catch (IOException err) {
                    res.send(err.toString());
                    getLogger().info("Could not find file");
                }
            } else {
                getLogger().info("Find file unsuccessful");
                res.send("Wrong hash");
            }
        });

        //TODO 192.168.178.26:3000/command/?hash=test&cmd=seed
        app.get("/command/", (req, res) -> {
            getLogger().info("Command request...");
            boolean checkHash = checkHash(req.getQuery("hash"));
            if(checkHash) {
                try {
                    Bukkit.getScheduler().runTask(this, () -> getServer().dispatchCommand(Bukkit.getConsoleSender(), URLDecoder.decode(req.getQuery("cmd"), StandardCharsets.UTF_8)));
                    res.send("Success");
                    getLogger().info("Success");
                } catch(CommandException | IllegalArgumentException err) {
                    res.send(err.toString());
                    getLogger().info("Cannot execute command");
                }
            } else {
                getLogger().info("Command unsuccessful");
                res.send("Wrong hash");
            }
        });

        //TODO 192.168.178.26:3000/chat/?hash=test&msg=bruhhhhh&username=Lianecx
        app.get("/chat/", (req, res) -> {
            getLogger().info("Chat request...");
            boolean checkHash = checkHash(req.getQuery("hash"));
            if(checkHash) {
                String msg = req.getQuery("msg");
                String username = req.getQuery("username");
                msg = URLDecoder.decode(msg, StandardCharsets.UTF_8);
                username = URLDecoder.decode(username, StandardCharsets.UTF_8);

                /*[
                    "",
                    {
                        "text":"Discord",
                        "bold":true,
                        "color":"aqua",
                        "clickEvent": {
                            "action":"open_url",
                            "value":"https://top.gg/bot/712759741528408064"
                        },
                        "hoverEvent": {
                            "action":"show_text",
                            "contents":
                            ["Message sent using \u00A76SMP-Bot"]
                        }
                    },
                    {
                        "text":" | ${message.member.user.tag}"
                        ,"bold":true
                    },
                    {
                        "text":" >> ",
                        "color":"dark_gray"
                    },
                    " ${chatMsg}"
                ]*/


                BaseComponent[] jsonMessage = ComponentSerializer.parse("[\"\",{\"text\":\"Discord\",\"bold\":true,\"color\":\"blue\",\"clickEvent\":{\"action\":\"open_url\",\"value\":\"https://top.gg/bot/712759741528408064\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"Message sent using \\u00A76SMP-Bot\"]}},{\"text\":\" | " + username + "\",\"bold\":true},{\"text\":\" >> \",\"color\":\"dark_gray\"},\"" + msg + "\"]");
                getServer().spigot().broadcast(jsonMessage);
                res.send("Success");
            } else {
                getLogger().info("Chat unsuccessful");
                res.send("Wrong hash");
            }
        });

        //TODO 192.168.178.26:3000/connect/?hash=VGhpc0lzUGxhaW5UZXh0&chat=true
        app.post("/connect/", (req, res) -> {
            getLogger().info("Connection request...");
            boolean checkConnection = checkConnection(req, req.getQuery("hash"));
            if(checkConnection) {
                try {
                    JsonObject connectJson = new JsonObject();
                    JsonObject parser = new JsonParser().parse(new InputStreamReader(req.getBody())).getAsJsonObject();
                    getLogger().info(parser.toString());
                    connectJson.addProperty("hash", parser.get("hash").getAsString());
                    connectJson.addProperty("chat", parser.get("chat").getAsString());
                    connectJson.addProperty("guild", parser.get("guild").getAsString());
                    if(parser.get("channel").getAsString() != null) connectJson.addProperty("channel", parser.get("channel").getAsString());
                    FileWriter writer = new FileWriter(getServer().getPluginManager().getPlugin("SMPBotPlugin").getDataFolder() + "/connection.conn");
                    writer.write(connectJson.toString());
                    writer.close();

                    JsonObject respJson = new JsonObject();
                    respJson.addProperty("ip", InetAddress.getLocalHost().getHostAddress() + ":" + getServer().getPort());
                    respJson.addProperty("version", getServer().getVersion());
                    respJson.addProperty("path", getServer().getWorldContainer().getCanonicalPath());

                    res.send(respJson.toString());
                    getLogger().info("Success");
                } catch (IOException err) {
                    res.send(err.toString());
                    getLogger().info("Cannot save hash");
                }
            } else {
                getLogger().info("Connection unsuccessful");
                res.send("Wrong hash-format or IP");
            }
        });

        //TODO 192.168.178.26:3000/
        app.get("/", (req, res) -> {
            res.send("Main Page!");
            getLogger().info("Root request.");
        });

        app.listen(() -> getLogger().info("Listening on port 3000"), 3000);
        return app;
    }

    public boolean checkHash(String hash) {
        try {
            Reader reader = Files.newBufferedReader(Paths.get(getServer().getPluginManager().getPlugin("SMPBotPlugin").getDataFolder() + "/connection.conn"));
            JsonObject parser = new JsonParser().parse(reader).getAsJsonObject();
            String correctHash = parser.get("hash").getAsString();
            reader.close();
            return correctHash.equals(hash);
        } catch(IOException | JsonParseException err) {
            return false;
        }
    }

    public boolean checkConnection(Request req, String hash) {
        //TODO add IP check
        return hash.matches("^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)?$");
    }
}
