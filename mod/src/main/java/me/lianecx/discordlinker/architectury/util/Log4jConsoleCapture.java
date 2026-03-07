package me.lianecx.discordlinker.architectury.util;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import static me.lianecx.discordlinker.common.util.ConsoleStreamCapture.shouldSkipDebugLog;

public final class Log4jConsoleCapture {

    private static final Object LOCK = new Object();

    private static LoggerContext loggerContext;
    private static Configuration configuration;
    private static LoggerConfig rootLogger;
    private static Appender appender;

    private Log4jConsoleCapture() {}

    public static void install(Consumer<String> lineConsumer) {
        synchronized(LOCK) {
            if(appender != null) return;

            loggerContext = (LoggerContext) LogManager.getContext(false);
            configuration = loggerContext.getConfiguration();
            rootLogger = configuration.getRootLogger();

            Layout<? extends Serializable> layout = findConsoleLayout(configuration);
            if(layout == null) layout = PatternLayout.newBuilder()
                        .withPattern("%d{HH:mm:ss} [%t/%level]: %msg%n%throwable")
                        .build();

            appender = new DiscordLinkerConsoleAppender("DiscordLinkerConsoleCapture", layout, lineConsumer);
            appender.start();
            configuration.addAppender(appender);
            rootLogger.addAppender(appender, null, null);
            loggerContext.updateLoggers();
        }
    }

    public static void uninstall() {
        synchronized(LOCK) {
            if(appender == null) return;

            rootLogger.removeAppender(appender.getName());
            configuration.getAppenders().remove(appender.getName());
            appender.stop();
            loggerContext.updateLoggers();

            appender = null;
            rootLogger = null;
            configuration = null;
            loggerContext = null;
        }
    }

    private static Layout<? extends Serializable> findConsoleLayout(Configuration configuration) {
        for(Appender existingAppender : configuration.getAppenders().values()) {
            if(existingAppender instanceof ConsoleAppender && existingAppender.getLayout() != null)
                return existingAppender.getLayout();
        }

        for(Appender existingAppender : configuration.getAppenders().values()) {
            if(existingAppender.getName() != null
                    && existingAppender.getName().toLowerCase().contains("console")
                    && existingAppender.getLayout() != null)
                return existingAppender.getLayout();
        }

        return null;
    }

    private static class DiscordLinkerConsoleAppender extends AbstractAppender {
        private final Consumer<String> lineConsumer;

        protected DiscordLinkerConsoleAppender(String name, Layout<? extends Serializable> layout, Consumer<String> lineConsumer) {
            super(name, null, layout, false);
            this.lineConsumer = lineConsumer;
        }

        @Override
        public void append(LogEvent event) {
            LogEvent immutable = event.toImmutable();
            // Only capture INFO and above to avoid excessive debug spam (probably disabled anyway)
            if(event.getLevel().intLevel() > Level.INFO.intLevel()) return;
            if(shouldSkipDebugLog(immutable.getLoggerName())) return;

            String formattedLine = new String(getLayout().toByteArray(immutable), StandardCharsets.UTF_8);
            lineConsumer.accept(formattedLine);
        }
    }
}
