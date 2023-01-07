package me.lianecx.discordlinker.network.adapters;

import io.socket.client.Socket;

import java.util.Arrays;

public class WebSocketAdapter {

    private final Socket socket;

    public WebSocketAdapter(Socket socket) {
        this.socket = socket;
    }

    public void connect() {
        socket.onAnyIncoming(args -> {
            System.out.println("Incoming: " + Arrays.toString(args));
        });

        socket.connect();
    }

    public void disconnect() {
        socket.disconnect();
    }
}
