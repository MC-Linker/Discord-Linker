package me.lianecx.discordlinker.spigot.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getLogger;

public class NBTParser {

    public static @Nullable String parsePlayerNBT(Player player) {
        try {
            // CraftPlayer
            Method getHandle = player.getClass().getMethod("getHandle");
            Object nmsPlayer = getHandle.invoke(player);

            // --- Create NBT compound ---
            Object nbtTag;

            Class<?> nbtClass;
            try {
                nbtClass = Class.forName("net.minecraft.server." + getNmsVersion() + ".NBTTagCompound");
            }
            catch(ClassNotFoundException e) {
                nbtClass = Class.forName("net.minecraft.server.NBTTagCompound");
            }
            nbtTag = nbtClass.getConstructor().newInstance();

            // --- Save player to NBT ---
            Method saveMethod = findSaveMethod(nmsPlayer.getClass(), nbtClass);
            saveMethod.invoke(nmsPlayer, nbtTag);

            return nbtTag.toString();
        }
        catch(Throwable t) {
            getLogger().debug("Failed to parse player NBT data: " + t.getMessage());
            return null;
        }
    }

    private static Method findSaveMethod(Class<?> entityClass, Class<?> nbtClass) throws NoSuchMethodException {
        // Mojang naming roulette, handle all common cases
        for(Method method : entityClass.getMethods()) {
            if(method.getParameterCount() == 1 && method.getParameterTypes()[0].equals(nbtClass)) {

                String name = method.getName();

                // Known save method names across versions
                if(name.equals("save") || name.equals("b") || name.equals("saveWithoutId"))
                    return method;
            }
        }
        throw new NoSuchMethodException("Could not find player NBT save method");
    }

    private static String getNmsVersion() {
        String name = Bukkit.getServer().getClass().getPackage().getName();
        return name.substring(name.lastIndexOf('.') + 1);
    }
}
