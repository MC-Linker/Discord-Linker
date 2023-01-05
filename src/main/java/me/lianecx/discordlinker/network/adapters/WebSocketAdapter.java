package me.lianecx.discordlinker.network.adapters;

import io.socket.client.Socket;

public class WebSocketAdapter {

    private final Socket socket;

    public WebSocketAdapter(Socket socket) {
        this.socket = socket;
    }

    public void connect() {
        socket.onAnyIncoming(System.out::println);

        socket.connect();
    }

    public void disconnect() {
        socket.disconnect();
    }
}
