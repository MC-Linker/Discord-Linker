package me.lianecx.discordlinker.common.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MinecraftVersionUtil {

    private static final Pattern NUMERIC_VERSION = Pattern.compile("^\\d+(?:\\.\\d+)*");

    private MinecraftVersionUtil() {}

    /**
     * Strips any build/snapshot suffix from a raw engine version string, keeping only the numeric
     * Minecraft version. Handles both the new build suffix (e.g. "26.1.2.build.53" ->; "26.1.2")
     * and the Bukkit release suffix (e.g. "26.1.2.build.53-R0.1-SNAPSHOT" ->; "26.1.2").
     */
    public static String normalize(String version) {
        Matcher matcher = NUMERIC_VERSION.matcher(version);
        return matcher.find() ? matcher.group() : version;
    }

    /**
     * Compares two Minecraft version strings (e.g. "1.20.1", "1.21").
     *
     * @return negative if v1 &lt; v2, zero if equal, positive if v1 &gt; v2
     */
    public static int compare(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int n1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int n2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            if (n1 != n2) return Integer.compare(n1, n2);
        }
        return 0;
    }

    /**
     * Returns true if {@code version} is greater than or equal to {@code minVersion}.
     */
    public static boolean isAtLeast(String version, String minVersion) {
        return compare(version, minVersion) >= 0;
    }

    /**
     * Returns true if {@code version} is less than or equal to {@code maxVersion}.
     */
    public static boolean isAtMost(String version, String maxVersion) {
        return compare(version, maxVersion) <= 0;
    }
}
