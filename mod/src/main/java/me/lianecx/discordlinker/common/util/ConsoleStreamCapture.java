package me.lianecx.discordlinker.common.util;

import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.logging.*;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getLogger;

public final class ConsoleStreamCapture {

    public static final String DISCORD_LINKER_LOGGER_TOKEN = "discordlinker";

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
                if(shouldSkipDebugDiscordLinkerRecord(record)) return;

                String formatted;
                try {
                    formatted = formatter.format(record);
                }
                catch(Exception e) {
                    formatted = record.getMessage();
                }

                emitLines(formatted, lineConsumer);
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

    private static void emitLines(String content, Consumer<String> lineConsumer) {
        if(content == null || content.isEmpty()) return;
        String[] split = content.split("\\r?\\n");
        for(String line : split) {
            if(line.trim().isEmpty()) continue;
            lineConsumer.accept(line);
        }
    }

    private static boolean shouldSkipDebugDiscordLinkerRecord(LogRecord record) {
        // Avoids feedback loops
        String loggerName = record.getLoggerName();
        if(loggerName == null || !loggerName.toLowerCase().contains(DISCORD_LINKER_LOGGER_TOKEN)) return false;
        return getLogger().isDebug(); // If debug enabled, skip all (debugs are emitted as info currently)
    }

    private interface UninstallBackend {
        void uninstall();
    }
}
