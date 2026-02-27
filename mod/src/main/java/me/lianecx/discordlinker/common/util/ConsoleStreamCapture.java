package me.lianecx.discordlinker.common.util;

import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.logging.*;

public final class ConsoleStreamCapture {

    private static final Object LOCK = new Object();

    private static boolean installed;
    private static UninstallBackend uninstallBackend;

    private ConsoleStreamCapture() {}

    public static void install(Consumer<String> lineConsumer) {
        synchronized(LOCK) {
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
        synchronized(LOCK) {
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
            // Reflection incase for some reason log4j isn't present
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
                if(!isLoggable(record)) return;

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
            if(line.isEmpty()) continue;
            lineConsumer.accept(line);
        }
    }

    private interface UninstallBackend {
        void uninstall();
    }
}
