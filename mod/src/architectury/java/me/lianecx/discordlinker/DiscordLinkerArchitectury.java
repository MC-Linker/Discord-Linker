package me.lianecx.discordlinker;

public class DiscordLinkerArchitectury extends DiscordLinkerCommon {

    private static DiscordLinkerArchitectury instance = null;

    public static DiscordLinkerArchitectury getInstance() {
        if(instance == null) instance = new DiscordLinkerArchitectury();
        return instance;
    }

    public static void init() {
        System.out.println("Hello from DiscordLinkerArchitectury!");
    }
}