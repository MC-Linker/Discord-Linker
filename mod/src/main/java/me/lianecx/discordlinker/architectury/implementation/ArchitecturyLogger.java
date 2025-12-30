package me.lianecx.discordlinker.architectury.implementation;

import me.lianecx.discordlinker.common.abstraction.core.LinkerLogger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ArchitecturyLogger implements LinkerLogger {

    private final Logger logger;

    public ArchitecturyLogger(String modId) {
        this.logger = LogManager.getLogger(modId);
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
        logger.debug(message);
    }
}