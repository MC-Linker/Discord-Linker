package me.lianecx.discordlinker.common.abstraction;

public class CommandCompletion {

    public final String text;
    public final int start;
    public final int end;

    public CommandCompletion(String text, int start, int end) {
        this.text = text;
        this.start = start;
        this.end = end;
    }
}