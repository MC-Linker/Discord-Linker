package me.lianecx.discordlinker.architectury.implementation;

import me.lianecx.discordlinker.architectury.DiscordLinkerMod;
import me.lianecx.discordlinker.common.abstraction.core.LinkerLogger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ModLogger extends LinkerLogger {

    private final Logger logger;

    public ModLogger(boolean debug) {
        this.logger = LogManager.getLogger(DiscordLinkerMod.MOD_ID);
        this.debug = debug;
    }

    @Override
    public void info(String message) {
        logger.info(message);
    }

    @Override
    public void warn(String message) {
        logger.warn(message);
    }

    @Override
    public void error(String message) {
        logger.error(message);
    }

    @Override
    public void debug(String message) {
        if(debug) logger.info(message);
    }
}