package me.lianecx.discordlinker.common.events;

import me.lianecx.discordlinker.common.util.MinecraftChatColor;

public final class JoinRequirementMessages {

    public static final String MISSING_REQUIRED_ROLE = MinecraftChatColor.RED + "You do not have the required role(s) to join this server.";
    public static final String ROLE_CHECK_FAILED = MinecraftChatColor.RED + "Your roles could not be verified. Please try again later.";
    public static final String IDENTITY_CHECK_FAILED = MinecraftChatColor.RED + "Could not verify your account identity. Please try again.";
    public static final String ROLE_CHECK_ERROR = MinecraftChatColor.RED + "Could not verify your account roles. Please try again later.";
    public static final String ROLE_CHECK_TIMED_OUT = MinecraftChatColor.RED + "Role verification timed out. Please try again.";
    public static final String ROLE_CHECK_INTERRUPTED = MinecraftChatColor.RED + "Role verification interrupted. Please try again.";

    private static final String NOT_CONNECTED_NO_URL_TEMPLATE =
            MinecraftChatColor.YELLOW + "You have not connected your Minecraft account to Discord.\nPlease DM " +
            MinecraftChatColor.AQUA + "@MC Linker#7784" + MinecraftChatColor.YELLOW + " with the code " +
            MinecraftChatColor.AQUA + "%d" + MinecraftChatColor.YELLOW +
            " in the next" + MinecraftChatColor.BOLD + " 3 minutes " + MinecraftChatColor.YELLOW + " and rejoin.";

    private static final String NOT_CONNECTED_WITH_URL_TEMPLATE =
            MinecraftChatColor.YELLOW + "You have not connected your Minecraft account to Discord.\nPlease join " +
            MinecraftChatColor.AQUA + MinecraftChatColor.UNDERLINE + "%s" + MinecraftChatColor.YELLOW + " and DM " +
            MinecraftChatColor.AQUA + "@MC Linker#7784" + MinecraftChatColor.YELLOW + " with the code " +
            MinecraftChatColor.AQUA + "%d" + MinecraftChatColor.YELLOW +
            " in the next" + MinecraftChatColor.BOLD + " 3 minutes." + MinecraftChatColor.YELLOW + " and rejoin.";

    private JoinRequirementMessages() {}

    public static String notConnected(int code, String url) {
        if(url == null) return String.format(NOT_CONNECTED_NO_URL_TEMPLATE, code);
        return String.format(NOT_CONNECTED_WITH_URL_TEMPLATE, url, code);
    }
}
