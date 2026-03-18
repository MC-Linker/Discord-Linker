package me.lianecx.discordlinker.common.abstraction;

public abstract class LinkerPlayer extends LinkerOfflinePlayer implements LinkerCommandSender {

    public LinkerPlayer(String uuid, String name) {
        super(uuid, name);
    }

    public abstract void sendMessageWithClickableURLs(String message);

    public abstract void kick(String reason);

    public abstract String getNBTAsString();

    public boolean isOnline() {
        return true;
    }
}
