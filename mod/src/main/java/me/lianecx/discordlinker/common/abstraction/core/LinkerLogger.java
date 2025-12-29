package me.lianecx.discordlinker.common.abstraction.core;

public interface LinkerLogger {
    void info(String message);

    void warn(String message);

    void error(String message);

    void debug(String message); // optional
}
