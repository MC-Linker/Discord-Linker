package me.lianecx.discordlinker.spigot.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getLogger;

public class NBTParser {

    private static volatile Method SAVE_METHOD;
    private static volatile Class<?> NBT_CLASS;

    public static @Nullable String parsePlayerNBT(Player player) {
        try {
            // CraftPlayer -> NMS player
            Method getHandle = player.getClass().getMethod("getHandle");
            Object nmsPlayer = getHandle.invoke(player);

            // Resolve & cache NBT class
            Class<?> nbtClass = getNbtClass();
            Object nbtTag = nbtClass.getConstructor().newInstance();

            // Resolve & cache save method
            Method saveMethod = getSaveMethod(nmsPlayer.getClass(), nbtClass);
            saveMethod.invoke(nmsPlayer, nbtTag);

            return nbtTag.toString();
        }
        catch (Throwable t) {
            getLogger().debug("Failed to parse player NBT data: " + t.getMessage());
            return null;
        }
    }

    private static Method getSaveMethod(Class<?> entityClass, Class<?> nbtClass) throws NoSuchMethodException {
        if (SAVE_METHOD != null) return SAVE_METHOD;

        for (Method method : entityClass.getMethods()) {
            if (method.getParameterCount() == 1 && method.getParameterTypes()[0].equals(nbtClass)) {
                String name = method.getName();
                if (name.equals("save") || name.equals("b") || name.equals("saveWithoutId")) {
                    method.setAccessible(true);
                    SAVE_METHOD = method;
                    return SAVE_METHOD;
                }
            }
        }

        throw new NoSuchMethodException("Could not find player NBT save method");
    }

    private static Class<?> getNbtClass() throws ClassNotFoundException {
        if (NBT_CLASS != null) return NBT_CLASS;

        try {
            NBT_CLASS = Class.forName("net.minecraft.server." + getNmsVersion() + ".NBTTagCompound");
        }
        catch (ClassNotFoundException e) {
            // Mojang-mapped / newer layouts fallback
            NBT_CLASS = Class.forName("net.minecraft.server.NBTTagCompound");
        }

        if(NBT_CLASS == null) throw new ClassNotFoundException("Could not find NBTTagCompound class");

        return NBT_CLASS;
    }

    private static String getNmsVersion() {
        String name = Bukkit.getServer().getClass().getPackage().getName();
        return name.substring(name.lastIndexOf('.') + 1);
    }
}
