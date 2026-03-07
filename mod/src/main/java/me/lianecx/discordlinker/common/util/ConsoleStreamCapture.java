package me.lianecx.discordlinker.common.util;

import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.logging.*;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getLogger;

public final class ConsoleStreamCapture {

    public static final String[] DISCORD_LINKER_LOGGER_TOKENS = { "discordlinker", "Discord-Linker" };

    private static final Object INSTALL_LOCK = new Object();

    private static boolean installed;
    private static UninstallBackend uninstallBackend;

    private ConsoleStreamCapture() {}

    public static void install(Consumer<String> lineConsumer) {
        synchronized(INSTALL_LOCK) {
            if(installed) return;

            UninstallBackend log4jUninstall = installLog4jIfPresent(lineConsumer);
            if(log4jUninstall != null) {
                uninstallBackend = log4jUninstall;
                installed = true;
                return;
            }

            uninstallBackend = installJul(lineConsumer);
            installed = true;
        }
    }

    public static void uninstall() {
        synchronized(INSTALL_LOCK) {
            if(!installed) return;

            if(uninstallBackend != null) {
                uninstallBackend.uninstall();
                uninstallBackend = null;
            }
            installed = false;
        }
    }

    private static UninstallBackend installLog4jIfPresent(Consumer<String> lineConsumer) {
        try {
            // Reflection incase on spigot
            Class<?> captureClass = Class.forName("me.lianecx.discordlinker.architectury.util.Log4jConsoleCapture");
            Method installMethod = captureClass.getMethod("install", Consumer.class);
            Method uninstallMethod = captureClass.getMethod("uninstall");

            installMethod.invoke(null, lineConsumer);
            return () -> {
                try {
                    uninstallMethod.invoke(null);
                }
                catch(Exception ignored) {}
            };
        }
        catch(Exception ignored) {
            return null;
        }
    }

    private static UninstallBackend installJul(Consumer<String> lineConsumer) {
        Logger rootLogger = LogManager.getLogManager().getLogger("");
        Formatter formatter = resolveFormatter(rootLogger);

        Handler captureHandler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                // Only capture INFO and above to avoid excessive debug spam (probably disabled anyway)
                if(!isLoggable(record) || record.getLevel().intValue() < Level.INFO.intValue()) return;
                if(shouldSkipDebugLog(record.getLoggerName())) return;

                String formatted;
                try {
                    formatted = formatter.format(record);
                }
                catch(Exception e) {
                    formatted = record.getMessage();
                }

                lineConsumer.accept(formatted);
            }

            @Override
            public void flush() {}

            @Override
            public void close() {}
        };

        captureHandler.setLevel(Level.ALL);
        rootLogger.addHandler(captureHandler);

        return () -> rootLogger.removeHandler(captureHandler);
    }

    private static Formatter resolveFormatter(Logger rootLogger) {
        Formatter fallback = new SimpleFormatter();

        for(Handler handler : rootLogger.getHandlers()) {
            if(handler == null) continue;
            Formatter formatter = handler.getFormatter();
            if(formatter == null) continue;

            if(handler.getClass().getName().toLowerCase().contains("console")) return formatter;
            fallback = formatter;
        }

        return fallback;
    }

    /**
     * @return true if debug mode is enabled and the record is from a DiscordLinker logger, to avoid feedback loops.
     */
    public static boolean shouldSkipDebugLog(String loggerName) {
        if(!isDiscordLinkerLogger(loggerName)) return false;
        return getLogger().isDebug(); // If debug enabled, skip all (debugs are emitted as info currently)
    }

    private static boolean isDiscordLinkerLogger(String name) {
        if(name == null) return false;

        String lower = name.toLowerCase();
        for(String token : DISCORD_LINKER_LOGGER_TOKENS) {
            if(lower.contains(token.toLowerCase())) return true;
        }
        return false;
    }

    private interface UninstallBackend {
        void uninstall();
    }
}
