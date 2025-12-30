package me.lianecx.discordlinker.architectury;

import me.lianecx.discordlinker.common.DiscordLinkerCommon;

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