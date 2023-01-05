package me.lianecx.discordlinker.network.adapters;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import express.Express;
import express.http.request.Request;
import express.http.response.Response;
import express.utils.Status;
import me.lianecx.discordlinker.DiscordLinker;
import me.lianecx.discordlinker.network.HttpConnection;
import me.lianecx.discordlinker.network.Route;
import me.lianecx.discordlinker.network.Router;

import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;

public class HttpAdapter {

    private final Express app;

    public HttpAdapter(Express app) {
        this.app = app;

        this.app.all((req, res) -> {
            if(!this.checkAuth(req)) return;

            Route route = Route.getRouteByPath(req.getPath());
            JsonObject data = this.parseRequest(req);
            System.out.println(data);
            System.out.println(route);

            if(route == Route.PUT_FILE) {
                this.respond(Router.putFile(data, req.getBody()), res);
                return;
            }

            if(route != null) this.respond(route.execute(data), res);
            else res.sendStatus(Status._404);
        });
    }

    public void connect(int port) {
        app.listen(() -> DiscordLinker.getPlugin().getLogger().info("Listening on port " + port), port);
    }

    private boolean checkAuth(Request req) {
        try {
            if(DiscordLinker.getConnJson().get("hash") == null) return false;

            String token = req.getAuthorization().get(0).getData();
            String correctHash = DiscordLinker.getConnJson().get("hash").getAsString();
            String correctIp = InetAddress.getByName(HttpConnection.BOT_URL.getHost()).getHostAddress();
            return correctHash.equals(Router.createHash(token)) && req.getIp().equals(correctIp);
        }
        catch(NoSuchAlgorithmException | UnknownHostException err) {
            return false;
        }
    }

    public void disconnect() {
        app.stop();
    }

    private void respond(Router.RouterResponse response, Response res) {
        res.setStatus(response.getStatus());

        if(response.isAttachment()) res.sendAttachment(Paths.get(response.getMessage()));
        else res.send(response.getMessage());
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
}
