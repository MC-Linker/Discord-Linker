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
            else {
                route.execute(data, routerResponse -> this.respond(routerResponse, res));
            }
        });
    }

    public void connect(int port) {
        app.listen(() -> DiscordLinker.getPlugin().getLogger().info("Listening on port " + port), port);
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

    private boolean checkIp(String ip) {
        try {
            String correctIp = InetAddress.getByName(HttpConnection.BOT_URL.getHost()).getHostAddress();
            return ip.equals(correctIp);
        }
        catch(UnknownHostException e) {
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
