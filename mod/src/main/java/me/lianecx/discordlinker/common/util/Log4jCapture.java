package me.lianecx.discordlinker.common.util;

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

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getLogger;

/**
 * Log4j2-based log capture shared across all platforms.
 * <p>
 * Provides two modes:
 * <ul>
 *   <li>{@link #install}/{@link #uninstall} – persistent capture for chat-console forwarding</li>
 *   <li>{@link #captureCommandOutput(Runnable)} – temporary capture for command output dispatch</li>
 * </ul>
 */
public final class Log4jCapture {

    public static final String[] DISCORD_LINKER_LOGGER_TOKENS = { "discordlinker", "Discord-Linker" };

    private static final Object LOCK = new Object();

    private static LoggerContext loggerContext;
    private static Configuration configuration;
    private static LoggerConfig rootLoggerConfig;
    private static Appender consoleAppender;

    private Log4jCapture() {}

    // ── Persistent console capture ──────────────────────────────

    public static void install(Consumer<String> lineConsumer) {
        synchronized(LOCK) {
            if(consoleAppender != null) return;

            ensureContext();

            Layout<? extends Serializable> layout = findConsoleLayout(configuration);
            if(layout == null) layout = PatternLayout.newBuilder()
                    .withPattern("%d{HH:mm:ss} [%t/%level]: %msg%n%throwable")
                    .build();

            consoleAppender = new ConsoleCaptureAppender("DiscordLinkerConsoleCapture", layout, lineConsumer);
            consoleAppender.start();
            configuration.addAppender(consoleAppender);
            rootLoggerConfig.addAppender(consoleAppender, null, null);
            loggerContext.updateLoggers();
        }
    }

    public static void uninstall() {
        synchronized(LOCK) {
            if(consoleAppender == null) return;

            rootLoggerConfig.removeAppender(consoleAppender.getName());
            configuration.getAppenders().remove(consoleAppender.getName());
            consoleAppender.stop();
            loggerContext.updateLoggers();

            consoleAppender = null;
            rootLoggerConfig = null;
            configuration = null;
            loggerContext = null;
        }
    }

    // ── Temporary command-output capture ────────────────────────

    /**
     * Attaches a temporary Log4j appender, executes the given action,
     * and returns every log message emitted during execution.
     */
    public static String captureCommandOutput(Runnable action) {
        StringBuilder output = new StringBuilder();

        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration cfg = ctx.getConfiguration();
        LoggerConfig root = cfg.getRootLogger();

        Appender tempAppender = new AbstractAppender("DiscordLinkerCommandCapture", null, null, false) {
            @Override
            public void append(LogEvent event) {
                String formatted = event.getMessage().getFormattedMessage();
                if(formatted != null && !formatted.isEmpty()) {
                    if(output.length() > 0) output.append('\n');
                    output.append(formatted);
                }
            }
        };

        tempAppender.start();
        cfg.addAppender(tempAppender);
        root.addAppender(tempAppender, null, null);
        ctx.updateLoggers();

        try {
            action.run();
        } finally {
            root.removeAppender(tempAppender.getName());
            cfg.getAppenders().remove(tempAppender.getName());
            tempAppender.stop();
            ctx.updateLoggers();
        }

        return output.toString();
    }

    // ── Helpers ─────────────────────────────────────────────────

    private static void ensureContext() {
        if(loggerContext == null) {
            loggerContext = (LoggerContext) LogManager.getContext(false);
            configuration = loggerContext.getConfiguration();
            rootLoggerConfig = configuration.getRootLogger();
        }
    }

    private static Layout<? extends Serializable> findConsoleLayout(Configuration config) {
        for(Appender a : config.getAppenders().values()) {
            if(a instanceof ConsoleAppender && a.getLayout() != null)
                return a.getLayout();
        }
        for(Appender a : config.getAppenders().values()) {
            if(a.getName() != null
                    && a.getName().toLowerCase().contains("console")
                    && a.getLayout() != null)
                return a.getLayout();
        }
        return null;
    }

    // ── Debug-log filtering ─────────────────────────────────────

    /**
     * @return true if debug mode is enabled and the record is from a DiscordLinker logger, to avoid feedback loops.
     */
    public static boolean shouldSkipDebugLog(String loggerName) {
        if(!isDiscordLinkerLogger(loggerName)) return false;
        return getLogger().isDebug();
    }

    private static boolean isDiscordLinkerLogger(String name) {
        if(name == null) return false;
        String lower = name.toLowerCase();
        for(String token : DISCORD_LINKER_LOGGER_TOKENS) {
            if(lower.contains(token.toLowerCase())) return true;
        }
        return false;
    }

    // ── Inner classes ───────────────────────────────────────────

    private static class ConsoleCaptureAppender extends AbstractAppender {
        private final Consumer<String> lineConsumer;

        protected ConsoleCaptureAppender(String name, Layout<? extends Serializable> layout, Consumer<String> lineConsumer) {
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
