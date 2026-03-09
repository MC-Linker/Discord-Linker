package me.lianecx.discordlinker.spigot.util;

import me.lianecx.discordlinker.common.abstraction.CommandCompletion;
import me.lianecx.discordlinker.common.util.MinecraftChatColor;
import org.bukkit.Bukkit;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getLogger;
import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getScheduler;

public final class SpigotCommandCompletionUtil {

    private static boolean initialized = false;

    private static boolean IS_BRIGADIER;

    // Cached objects
    private static Object MINECRAFT_SERVER;

    private static Object DISPATCHER;

    // Legacy (1.12)
    private static Object COMMAND_HANDLER;
    private static Method LEGACY_TAB_COMPLETE;

    // Brigadier (1.13+)
    private static Method CREATE_COMMAND_SOURCE_METHOD;
    private static Method PARSE_METHOD;
    private static Method SUGGEST_METHOD;
    private static Method GET_LIST_METHOD;
    private static Method GET_TEXT_METHOD;
    private static Method GET_RANGE_METHOD;
    private static Method RANGE_GET_START_METHOD;
    private static Method RANGE_GET_END_METHOD;

    private SpigotCommandCompletionUtil() {}

    static {
        try {
            Object craftServer = Bukkit.getServer();
            MINECRAFT_SERVER = craftServer.getClass().getMethod("getServer").invoke(craftServer);

            // Determine server version to decide brigadier vs legacy
            IS_BRIGADIER = isBrigadierSupported();

            if (IS_BRIGADIER) {
                Class<?> brigadierDispatcherClass = Class.forName("com.mojang.brigadier.CommandDispatcher");

                // Find NMS Commands object from MinecraftServer
                // Mojang-mapped: getCommands() | Spigot-mapped: may differ
                Object commands = findAndInvoke(MINECRAFT_SERVER, rt -> hasNoArgMethodReturning(rt, brigadierDispatcherClass), "getCommands");

                // Fallback: CraftServer often exposes getCommandDispatcher()
                if (commands == null) {
                    commands = findAndInvoke(craftServer, rt -> hasNoArgMethodReturning(rt, brigadierDispatcherClass), "getCommandDispatcher");
                }

                if (commands == null) throw new ReflectiveOperationException("Cannot find Commands object on MinecraftServer or CraftServer");

                // Find brigadier CommandDispatcher from the NMS Commands wrapper
                DISPATCHER = findAndInvoke(commands, brigadierDispatcherClass::isAssignableFrom, "getDispatcher");

                if (DISPATCHER == null) throw new ReflectiveOperationException("Cannot find getDispatcher on " + commands.getClass().getName());

                // Find createCommandSourceStack on MinecraftServer (NOT on Commands)
                CREATE_COMMAND_SOURCE_METHOD = findNoArgMethod(
                        MINECRAFT_SERVER.getClass(),
                        rt -> rt.getSimpleName().contains("CommandSource") || rt.getSimpleName().contains("CommandListener"),
                        "createCommandSourceStack", "createCommandListenerWrapper"
                );

                if(CREATE_COMMAND_SOURCE_METHOD == null) throw new ReflectiveOperationException("Cannot find createCommandSourceStack on " + MINECRAFT_SERVER.getClass().getName());

                // Brigadier methods use erased types (generics → Object) and stable names
                PARSE_METHOD = DISPATCHER.getClass().getMethod("parse", String.class, Object.class);
                SUGGEST_METHOD = DISPATCHER.getClass().getMethod("getCompletionSuggestions", Class.forName("com.mojang.brigadier.ParseResults"));

                // Resolve suggestion accessors once
                Class<?> suggestionsClass = Class.forName("com.mojang.brigadier.suggestion.Suggestions");
                GET_LIST_METHOD = suggestionsClass.getMethod("getList");

                Class<?> suggestionClass = Class.forName("com.mojang.brigadier.suggestion.Suggestion");
                GET_TEXT_METHOD = suggestionClass.getMethod("getText");
                GET_RANGE_METHOD = suggestionClass.getMethod("getRange");

                Class<?> stringRangeClass = Class.forName("com.mojang.brigadier.context.StringRange");
                RANGE_GET_START_METHOD = stringRangeClass.getMethod("getStart");
                RANGE_GET_END_METHOD = stringRangeClass.getMethod("getEnd");

            } else {
                // 1.12.x legacy path — versioned NMS packages still exist here
                String pkg = craftServer.getClass().getPackage().getName();
                String nmsVersion = pkg.substring(pkg.lastIndexOf('.') + 1);

                Class<?> iCommandSender = Class.forName("net.minecraft.server." + nmsVersion + ".ICommandSender");
                Class<?> blockPosition = Class.forName("net.minecraft.server." + nmsVersion + ".BlockPosition");

                COMMAND_HANDLER = MINECRAFT_SERVER.getClass().getMethod("getCommandHandler").invoke(MINECRAFT_SERVER);
                LEGACY_TAB_COMPLETE = COMMAND_HANDLER.getClass().getMethod("getTabCompletions", iCommandSender, String.class, blockPosition);
            }

            initialized = true;
        } catch (Exception e) {
            System.out.println(MinecraftChatColor.YELLOW + "Failed to initialize command completion reflection");
            e.printStackTrace();
        }
    }

    /**
     * Determines whether the server supports brigadier (1.13+).
     * Checks for the presence of the brigadier library on the classpath.
     */
    private static boolean isBrigadierSupported() {
        try {
            Class.forName("com.mojang.brigadier.CommandDispatcher");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static CompletableFuture<List<CommandCompletion>> getCommandCompletions(String partialCommand, int limit) {
        if (!initialized) {
            getLogger().debug("Command completion not initialized, returning empty list");
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        CompletableFuture<List<CommandCompletion>> future = new CompletableFuture<>();

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
    private static List<CommandCompletion> getLegacyCompletions(String input, int limit) throws Exception {
        String normalizedInput = input.startsWith("/") ? input.substring(1) : input;
        int rangeStart = getFallbackRangeStart(normalizedInput);
        int rangeEnd = normalizedInput.length();

        if (!input.startsWith("/")) input = "/" + input;

        return ((List<?>) LEGACY_TAB_COMPLETE
                .invoke(COMMAND_HANDLER, Bukkit.getServer().getConsoleSender(), input, null))
                .stream()
                .limit(limit)
                .map(String::valueOf)
                .map(text -> new CommandCompletion(text, rangeStart, rangeEnd))
                .collect(Collectors.toList());
    }

    /* ===================== 1.13+ =================== */

    private static CompletableFuture<List<CommandCompletion>> getBrigadierCompletions(String input, int limit) throws Exception {
        if (input.startsWith("/")) input = input.substring(1);

        // Create a fresh CommandSourceStack per request to avoid stale state
        Object commandSource = CREATE_COMMAND_SOURCE_METHOD.invoke(MINECRAFT_SERVER);

        Object parseResults = PARSE_METHOD.invoke(DISPATCHER, input, commandSource);

        CompletableFuture<?> suggestionsFuture = (CompletableFuture<?>) SUGGEST_METHOD.invoke(DISPATCHER, parseResults);

        return suggestionsFuture.thenApply(suggestions -> {
            try {
                List<?> suggestionList = (List<?>) GET_LIST_METHOD.invoke(suggestions);

                return suggestionList.stream()
                        .limit(limit)
                        .map(SpigotCommandCompletionUtil::toCommandCompletion)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

            } catch (Exception e) {
                getLogger().debug("Suggestion parsing failed: " + e.getMessage());
                return Collections.emptyList();
            }
        });
    }

    private static CommandCompletion toCommandCompletion(Object suggestion) {
        try {
            String text = (String) GET_TEXT_METHOD.invoke(suggestion);
            Object range = GET_RANGE_METHOD.invoke(suggestion);
            int start = (int) RANGE_GET_START_METHOD.invoke(range);
            int end = (int) RANGE_GET_END_METHOD.invoke(range);
            return new CommandCompletion(text, start, end);
        } catch (Exception e) {
            return null;
        }
    }

    private static int getFallbackRangeStart(String input) {
        if (input.isEmpty()) return 0;
        int lastSpace = input.lastIndexOf(' ');
        return lastSpace == -1 ? 0 : lastSpace + 1;
    }

    /* ================ Reflection helpers ================ */

    /**
     * Finds and invokes a no-arg method on the target object.
     * Tries each preferred name first, then searches all public no-arg methods
     * by return type predicate. Returns null if no matching method is found.
     */
    private static Object findAndInvoke(Object target, Predicate<Class<?>> returnTypePredicate, String... preferredNames) throws Exception {
        Method method = findNoArgMethod(target.getClass(), returnTypePredicate, preferredNames);
        return method != null ? method.invoke(target) : null;
    }

    /**
     * Finds a no-arg public method on the given class.
     * Tries each preferred name first, then searches all public no-arg methods
     * whose return type matches the predicate.
     */
    private static Method findNoArgMethod(Class<?> clazz, Predicate<Class<?>> returnTypePredicate, String... preferredNames) {
        for (String name : preferredNames) {
            try {
                Method m = clazz.getMethod(name);
                if (m.getParameterCount() == 0 && returnTypePredicate.test(m.getReturnType())) {
                    return m;
                }
            } catch (NoSuchMethodException ignored) {}
        }

        for (Method m : clazz.getMethods()) {
            if (m.getParameterCount() == 0 && returnTypePredicate.test(m.getReturnType())) {
                return m;
            }
        }

        return null;
    }

    /**
     * Checks if the given class has any public no-arg method whose return type
     * is assignable to the specified type.
     */
    private static boolean hasNoArgMethodReturning(Class<?> clazz, Class<?> returnType) {
        for (Method m : clazz.getMethods()) {
            if (m.getParameterCount() == 0 && returnType.isAssignableFrom(m.getReturnType())) {
                return true;
            }
        }
        return false;
    }
}