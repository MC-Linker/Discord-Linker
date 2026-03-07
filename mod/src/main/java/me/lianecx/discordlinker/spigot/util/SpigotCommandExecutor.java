package me.lianecx.discordlinker.spigot.util;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.lang.reflect.Proxy;
import java.util.concurrent.CompletableFuture;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getScheduler;
import static me.lianecx.discordlinker.common.abstraction.LinkerServer.COMMAND_NO_OUTPUT_FAIL;
import static me.lianecx.discordlinker.common.abstraction.LinkerServer.COMMAND_NO_OUTPUT_SUCCESS;

/**
 * Executes server commands and captures their output.
 * <p>
 * Dispatches via {@link Bukkit#dispatchCommand} with a Log4j2
 * appender attached to the root logger to capture vanilla command output.
 */
public final class SpigotCommandExecutor {

    private SpigotCommandExecutor() {}

    /**
     * Executes a command and returns the output.
     */
    public static CompletableFuture<String> execute(String command) {
        CompletableFuture<String> future = new CompletableFuture<>();

        getScheduler().runSync(() -> {
            StringBuilder output = new StringBuilder();

            Object appender = null;
            Object log4jLogger = null;
            boolean success;
            try {
                Object[] capture = attachLog4jAppender(output);
                appender = capture[0];
                log4jLogger = capture[1];

                success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }
            catch(Exception e) {
                success = false;
            }
            finally {
                detachLog4jAppender(appender, log4jLogger);
            }

            String out = output.toString();
            if (out.isEmpty()) out = success ? COMMAND_NO_OUTPUT_SUCCESS : COMMAND_NO_OUTPUT_FAIL;
            future.complete(out);
        });

        return future;
    }

    /**
     * Attaches a Log4j2 appender to the root logger via reflection to capture command output.
     * Log4j2 is always present at runtime on Minecraft servers but not a compile dependency.
     * Returns [appender, logger] for cleanup.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object[] attachLog4jAppender(StringBuilder output) {
        try {
            Class<?> logManagerClass = Class.forName("org.apache.logging.log4j.LogManager");
            Object rootLogger = logManagerClass.getMethod("getRootLogger").invoke(null);
            Class<?> appenderClass = Class.forName("org.apache.logging.log4j.core.Appender");

            Object appender = Proxy.newProxyInstance(
                    appenderClass.getClassLoader(),
                    new Class[]{appenderClass},
                    (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "append":
                                if (args != null && args.length > 0 && args[0] != null) {
                                    Object event = args[0];
                                    Object message = event.getClass().getMethod("getMessage").invoke(event);
                                    if (message != null) {
                                        String formatted = (String) message.getClass().getMethod("getFormattedMessage").invoke(message);
                                        if (formatted != null && !formatted.isEmpty()) {
                                            if (output.length() > 0) output.append('\n');
                                            output.append(formatted);
                                        }
                                    }
                                }
                                return null;
                            case "getName":
                            case "toString":
                                return "DiscordLinkerCapture";
                            case "isStarted":
                            case "ignoringExceptions":
                                return true;
                            case "isStopped":
                            case "isFiltered":
                                return false;
                            case "getState":
                                try {
                                    Class stateClass = Class.forName("org.apache.logging.log4j.core.LifeCycle$State");
                                    return Enum.valueOf(stateClass, "STARTED");
                                } catch (Exception e) {
                                    return null;
                                }
                            case "hashCode":
                                return System.identityHashCode(proxy);
                            case "equals":
                                return proxy == args[0];
                            default:
                                return null;
                        }
                    }
            );

            rootLogger.getClass().getMethod("addAppender", appenderClass).invoke(rootLogger, appender);
            return new Object[]{ appender, rootLogger };
        } catch (Exception e) {
            return new Object[]{ null, null };
        }
    }

    /**
     * Removes the previously attached Log4j2 appender.
     */
    private static void detachLog4jAppender(Object appender, Object logger) {
        if (appender == null || logger == null) return;
        try {
            Class<?> appenderClass = Class.forName("org.apache.logging.log4j.core.Appender");
            logger.getClass().getMethod("removeAppender", appenderClass).invoke(logger, appender);
        } catch (Exception ignored) {}
    }
}
