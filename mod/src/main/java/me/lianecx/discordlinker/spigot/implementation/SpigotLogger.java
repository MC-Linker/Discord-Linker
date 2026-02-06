package me.lianecx.discordlinker.spigot.implementation;

import me.lianecx.discordlinker.common.abstraction.core.LinkerLogger;
import org.bukkit.Bukkit;

import java.util.logging.Logger;

public class SpigotLogger extends LinkerLogger {

    private final Logger logger;

    public SpigotLogger(Logger logger, boolean debug) {
        this.logger = logger;
        this.debug = debug;
    }

    @Override
    public void info(String message) {
        logger.info(message);
    }

    @Override
    public void warn(String message) {
        logger.warning(message);
    }

    @Override
    public void error(String message) {
        logger.severe(message);
    }

    @Override
    public void debug(String message) {
        if(debug) logger.info(message);
    }
}
