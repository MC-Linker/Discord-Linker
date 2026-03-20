package me.lianecx.discordlinker.architectury.hybrid;

import me.lianecx.discordlinker.architectury.implementation.ModPlayer;
import me.lianecx.discordlinker.common.events.data.ChatEventData;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getMinecraftEventBus;

/**
 * Bridges Bukkit chat events to the LinkerMinecraftEventBus on hybrid servers (Mohist, Magma, Arclight, etc.).
 * All Bukkit interaction is done via reflection since Bukkit is not a compile-time dependency for mod builds.
 */
public class BukkitEventBridge {

    private static Boolean hybridServer;

    private BukkitEventBridge() {}

    public static boolean isHybridServer() {
        if (hybridServer == null) {
            try {
                Class.forName("org.bukkit.Bukkit");
                hybridServer = true;
            } catch (ClassNotFoundException e) {
                hybridServer = false;
            }
        }
        return hybridServer;
    }

    public static void register() {
        try {
            // Load Bukkit classes
            Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
            Class<?> eventClass = Class.forName("org.bukkit.event.player.AsyncPlayerChatEvent");
            Class<?> listenerClass = Class.forName("org.bukkit.event.Listener");
            Class<?> priorityClass = Class.forName("org.bukkit.event.EventPriority");
            Class<?> executorClass = Class.forName("org.bukkit.plugin.EventExecutor");
            Class<?> pluginClass = Class.forName("org.bukkit.plugin.Plugin");

            // Get EventPriority.MONITOR
            Object monitorPriority = priorityClass.getField("MONITOR").get(null);

            // Get PluginManager
            Method getPluginManager = bukkitClass.getMethod("getPluginManager");
            Object pluginManager = getPluginManager.invoke(null);

            // Get a plugin reference (any loaded plugin works for event registration)
            Method getPlugins = pluginManager.getClass().getMethod("getPlugins");
            Object[] plugins = (Object[]) getPlugins.invoke(pluginManager);
            if (plugins.length == 0) {
                System.err.println("[DiscordLinker] BukkitEventBridge: No plugins loaded, cannot register Bukkit events");
                return;
            }
            Object plugin = plugins[0];

            // Create a Listener proxy (marker interface, no methods to implement)
            Object listener = Proxy.newProxyInstance(
                    listenerClass.getClassLoader(),
                    new Class[]{listenerClass},
                    (proxy, method, args) -> {
                        // Listener is a marker interface; delegate Object methods
                        switch(method.getName()) {
                            case "hashCode":
                                return System.identityHashCode(proxy);
                            case "equals":
                                return proxy == args[0];
                            case "toString":
                                return "BukkitEventBridge$Listener";
                        }
                        return null;
                    }
            );

            // Create an EventExecutor proxy that handles the chat event
            Object executor = Proxy.newProxyInstance(
                    executorClass.getClassLoader(),
                    new Class[]{executorClass},
                    new ChatEventHandler()
            );

            // Register: pluginManager.registerEvent(eventClass, listener, priority, executor, plugin, ignoreCancelled)
            Method registerEvent = pluginManager.getClass().getMethod(
                    "registerEvent", Class.class, listenerClass, priorityClass, executorClass, pluginClass, boolean.class
            );
            registerEvent.invoke(pluginManager, eventClass, listener, monitorPriority, executor, plugin, true);

            System.out.println("[DiscordLinker] BukkitEventBridge: Registered Bukkit chat event listener on hybrid server");
        } catch (Exception e) {
            System.err.println("[DiscordLinker] BukkitEventBridge: Failed to register Bukkit events");
            e.printStackTrace();
        }
    }

    private static class ChatEventHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if (!"execute".equals(method.getName())) return null;

            // args[0] = Listener, args[1] = Event (AsyncPlayerChatEvent)
            Object event = args[1];
            try {
                // Get message
                String message = (String) event.getClass().getMethod("getMessage").invoke(event);

                // Get Bukkit Player
                Object bukkitPlayer = event.getClass().getMethod("getPlayer").invoke(event);

                // Get ServerPlayer via CraftPlayer.getHandle()
                Object handle = bukkitPlayer.getClass().getMethod("getHandle").invoke(bukkitPlayer);
                if (!(handle instanceof ServerPlayer)) return null;

                ServerPlayer serverPlayer = (ServerPlayer) handle;
                getMinecraftEventBus().emit(new ChatEventData(message, new ModPlayer(serverPlayer)));
            } catch (Exception e) {
                System.err.println("[DiscordLinker] BukkitEventBridge: Error handling chat event");
                e.printStackTrace();
            }
            return null;
        }
    }
}
