package me.lianecx.discordlinker.spigot.util;

import org.bukkit.Bukkit;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getLogger;
import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getScheduler;

public final class SpigotCommandCompletionUtil {

    private static final String NMS_VERSION;
    private static final boolean IS_BRIGADIER;

    // Cached objects
    private static final Object MINECRAFT_SERVER;

    private static Object DISPATCHER;
    private static Object COMMAND_SOURCE;

    // Cached methods
    private static Method LEGACY_TAB_COMPLETE;

    private static Method PARSE_METHOD;
    private static Method SUGGEST_METHOD;
    private static Method GET_LIST_METHOD;
    private static Method GET_TEXT_METHOD;

    private SpigotCommandCompletionUtil() {}

    static {
        try {
            String pkg = Bukkit.getServer().getClass().getPackage().getName();
            NMS_VERSION = pkg.substring(pkg.lastIndexOf('.') + 1);
            IS_BRIGADIER = !NMS_VERSION.startsWith("v1_12");

            Object craftServer = Bukkit.getServer();
            MINECRAFT_SERVER = craftServer.getClass().getMethod("getServer").invoke(craftServer);

            if (IS_BRIGADIER) {
                // Commands instance
                Object commands = MINECRAFT_SERVER.getClass().getMethod("getCommands").invoke(MINECRAFT_SERVER);

                DISPATCHER = commands.getClass().getMethod("getDispatcher").invoke(commands);

                COMMAND_SOURCE = commands.getClass().getMethod("createCommandSourceStack").invoke(commands);

                PARSE_METHOD = DISPATCHER.getClass().getMethod("parse", String.class, COMMAND_SOURCE.getClass());

                SUGGEST_METHOD = DISPATCHER.getClass().getMethod("getCompletionSuggestions", Class.forName("com.mojang.brigadier.ParseResults"));

                // Resolve suggestion accessors once
                Class<?> suggestionsClass = Class.forName("com.mojang.brigadier.suggestion.Suggestions");
                GET_LIST_METHOD = suggestionsClass.getMethod("getList");

                Class<?> suggestionClass = Class.forName("com.mojang.brigadier.suggestion.Suggestion");
                GET_TEXT_METHOD = suggestionClass.getMethod("getText");

            } else {
                Object commandHandler = MINECRAFT_SERVER.getClass().getMethod("getCommandHandler").invoke(MINECRAFT_SERVER);
                LEGACY_TAB_COMPLETE = commandHandler.getClass().getMethod("getTabCompletions", getNmsClass("ICommandSender"), String.class, getNmsClass("BlockPosition"));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize command completion reflection", e);
        }
    }

    public static CompletableFuture<List<String>> getCommandCompletions(String partialCommand, int limit) {
        CompletableFuture<List<String>> future = new CompletableFuture<>();

        getScheduler().runSync(() -> {
            try {
                if (IS_BRIGADIER) {
                    getBrigadierCompletions(partialCommand, limit)
                        .thenAccept(future::complete)
                        .exceptionally(ex -> {
                            getLogger().debug("Brigadier completion failed: " + ex.getMessage());
                            future.complete(Collections.emptyList());
                            return null;
                        });
                } else {
                    future.complete(getLegacyCompletions(partialCommand, limit));
                }
            } catch (Throwable t) {
                getLogger().debug("Completion failed for input '" + partialCommand + "': " + t.getMessage());
                future.complete(Collections.emptyList());
            }
        });

        return future;
    }

    /* ===================== 1.12 ======================= */

    @SuppressWarnings("unchecked")
    private static List<String> getLegacyCompletions(String input, int limit) throws Exception {
        if (!input.startsWith("/")) input = "/" + input;

        return (List<String>) LEGACY_TAB_COMPLETE
            .invoke(MINECRAFT_SERVER, Bukkit.getServer().getConsoleSender(), input, null)
            .stream()
            .limit(limit)
            .collect(Collectors.toList());
    }

    /* ===================== 1.13+ =================== */

    private static CompletableFuture<List<String>> getBrigadierCompletions(String input, int limit) throws Exception {
        if (input.startsWith("/")) input = input.substring(1);

        Object parseResults = PARSE_METHOD.invoke(DISPATCHER, input, COMMAND_SOURCE);

        CompletableFuture<?> suggestionsFuture = (CompletableFuture<?>) SUGGEST_METHOD.invoke(DISPATCHER, parseResults);

        return suggestionsFuture.thenApply(suggestions -> {
            try {
                List<?> suggestionList = (List<?>) GET_LIST_METHOD.invoke(suggestions);

                return suggestionList.stream()
                    .limit(limit)
                    .map(s -> {
                        try {
                            return (String) GET_TEXT_METHOD.invoke(s);
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            } catch (Exception e) {
                getLogger().debug("Suggestion parsing failed: " + e.getMessage());
                return Collections.emptyList();
            }
        });
    }

    private static Class<?> getNmsClass(String name) throws ClassNotFoundException {
        return Class.forName("net.minecraft.server." + NMS_VERSION + "." + name);
    }
}