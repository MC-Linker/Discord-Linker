package me.lianecx.discordlinker.network.adapters;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import express.Express;
import express.http.RequestMethod;
import express.http.request.Request;
import express.http.response.Response;
import express.utils.Status;
import me.lianecx.discordlinker.DiscordLinker;
import me.lianecx.discordlinker.network.Route;
import me.lianecx.discordlinker.network.Router;
import org.bukkit.ChatColor;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Deprecated
public class HttpAdapter implements NetworkAdapter {

    private final Express app;

    public HttpAdapter() {
        this.app = new Express();

        this.app.all((req, res) -> {
            Route route = Route.getRouteByPath(req.getPath());
            if(route == null) {
                res.sendStatus(Status._404);
                return;
            }

            //Check auth and IP
            if((route.doesRequireToken() && !this.checkToken(req)) || (route.isBotOnly() && !this.checkIp(req.getIp()))) {
                res.setStatus(Status._401);
                res.send(Router.INVALID_AUTH.toString());
                return;
            }
            JsonObject data = this.parseRequest(req);

            if(route == Route.PUT_FILE) {
                //Special case: File upload (pass body as input stream to function)
                Router.putFile(data, req.getBody(), routerResponse -> this.respond(routerResponse, res));
            }
            else if(route == Route.ROOT) {
                res.redirect("https://mclinker.com");
            }
            else {
                route.execute(data, routerResponse -> this.respond(routerResponse, res));
            }
        });
    }

    @Deprecated
    public void connect(int port, Consumer<Boolean> callback) {
/*        app.listen(() -> {
            callback.accept(true);
            DiscordLinker.getPlugin().getLogger().info("Listening on port " + port);
        }, port);*/
    }

    public static Router.RouterResponse send(RequestMethod method, String route, JsonElement body) {
        try {
            if(body.isJsonObject()) {
                body.getAsJsonObject().add("id", DiscordLinker.getConnJson().get("id"));
                body.getAsJsonObject().add("ip", DiscordLinker.getConnJson().get("ip"));
            }

            HttpURLConnection conn = (HttpURLConnection) new URL(AdapterManager.BOT_URI + route).openConnection();

            byte[] out = body.toString().getBytes(StandardCharsets.UTF_8);
            int length = out.length;

            conn.setRequestMethod(method.getMethod());
            conn.setRequestProperty("Content-type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + DiscordLinker.getConnJson().get("hash").getAsString());
            conn.setFixedLengthStreamingMode(length);
            conn.setDoOutput(true);

            conn.connect();
            try(OutputStream os = conn.getOutputStream()) {
                os.write(out);
            }

            int status = conn.getResponseCode();
            //Bot could not find a valid connection to this server
            if(status == 403) DiscordLinker.getPlugin().deleteConn();
            Reader streamReader = new InputStreamReader(status > 299 ? conn.getErrorStream() : conn.getInputStream());
            return new Router.RouterResponse(Status.valueOf(status), new BufferedReader(streamReader).lines().collect(Collectors.joining("\n")));
        }
        catch(IOException ignored) {
            return null;
        }
    }

    private boolean checkToken(Request req) {
        try {
            if(DiscordLinker.getConnJson().get("hash") == null) return false;

            String token = req.getAuthorization().get(0).getData();
            String correctHash = DiscordLinker.getConnJson().get("hash").getAsString();
            return correctHash.equals(Router.createHash(token));
        }
        catch(NoSuchAlgorithmException err) {
            return false;
        }
    }

    private JsonObject parseRequest(Request req) {
        JsonObject data = new JsonObject();

        //GET requests can't have bodies
        //PUT request bodies are file streams
        if(!req.getMethod().equals("GET") && !req.getMethod().equals("PUT"))
            data = new JsonParser().parse(new InputStreamReader(req.getBody())).getAsJsonObject();
        //Parse query
        req.getQuerys().forEach(data::addProperty);
        //Parse params
        req.getParams().forEach(data::addProperty);

        return data;
    }

    public static void checkVersion() {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(AdapterManager.BOT_URI + "/version").openConnection();
            InputStream inputStream = conn.getInputStream();
            String latestVersion = new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining("\n"));
            if(!latestVersion.equals(DiscordLinker.getPluginVersion()))
                DiscordLinker.getPlugin().getLogger().info(ChatColor.AQUA + "Please update to the latest Discord-Linker version (" + latestVersion + ") for a bug-free and feature-rich experience.");

        }
        catch(IOException ignored) {}
    }

    public void disconnect() {
        app.stop();
    }

    private boolean checkIp(String ip) {
        try {
            String correctIp = InetAddress.getByName(AdapterManager.BOT_URI.getHost()).getHostAddress();
            return ip.equals(correctIp);
        }
        catch(UnknownHostException e) {
            return false;
        }
    }

    private void respond(Router.RouterResponse response, Response res) {
        res.setStatus(response.getStatus());

        if(response.isAttachment()) res.sendAttachment(Paths.get(response.getMessage()));
        else res.send(response.getMessage());
    }
}
