package me.lianecx.discordlinker.architectury.implementation;

import com.mojang.authlib.GameProfile;
import dev.architectury.platform.Platform;
import me.lianecx.discordlinker.common.abstraction.LinkerOfflinePlayer;
import me.lianecx.discordlinker.common.abstraction.LinkerPlayer;
import me.lianecx.discordlinker.common.abstraction.LinkerServer;
import me.lianecx.discordlinker.common.util.YamlUtil;
//? if <1.19 {
/*import net.minecraft.Util;
import org.jetbrains.annotations.NotNull;
*///? }
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
//? if >=1.21 {
/*import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.players.NameAndId;
*///? }
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.nodes.MappingNode;

import net.minecraft.ChatFormatting;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static me.lianecx.discordlinker.architectury.util.URLComponent.buildURLComponent;
import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getLogger;

public final class ModServer implements LinkerServer {

    private final MinecraftServer server;

    public ModServer(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public String getServerName() {
        return server.getServerModName();
    }

    @Override
    public int getMaxPlayers() {
        return server.getMaxPlayers();
    }

    @Override
    public int getOnlinePlayersCount() {
        return server.getPlayerList().getPlayers().size();
    }

    @Override
    public List<LinkerPlayer> getOnlinePlayers() {
        return server.getPlayerList().getPlayers()
                .stream()
                .map(ModPlayer::new)
                .collect(Collectors.toList());
    }

    @Override
    public String getDataFolder() {
        return Platform.getConfigFolder().resolve("discordlinker").toString();
    }

    @Override
    public LinkerPlayer getPlayer(UUID uuid) {
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if(player == null) return null;
        return new ModPlayer(player);
    }

    @Override
    public LinkerOfflinePlayer getOfflinePlayer(String name) {
        // Player is online
        ServerPlayer online = server.getPlayerList().getPlayerByName(name);
        if(online != null) return new ModPlayer(online);

        // Player is offline

        //? if <1.21 {
        //TODO test if this is null in offline mode
        if(server.getProfileCache() == null) return null;

        // may block (creates offline profile if couldn't fetch)
        GameProfile profile = server.getProfileCache()
                //? if <1.18 {
                /*.get(name);
                *///? } else {
                .get(name)
                .orElse(null);
                //? }
        if(profile != null) return new LinkerOfflinePlayer(profile.getId().toString(), profile.getName());
        //? } else {
        /*UUID uuid = server.services().nameToIdCache().get(name)
                .map(NameAndId::id)
                .orElse(null);
        if(uuid != null) return new LinkerOfflinePlayer(uuid.toString(), name);
        *///? }
        return null;
    }

    @Override
    public LinkerOfflinePlayer getOfflinePlayer(UUID uuid) {
        // Player is online
        LinkerPlayer online = getPlayer(uuid);
        if(online != null) return online;

        // Player is offline

        //? if <1.21 {
        //TODO test if this is null in offline mode
        if(server.getProfileCache() == null) return null;

        GameProfile profile = server.getProfileCache()
                //? if <1.18 {
                /*.get(uuid);
                *///? } else {
                .get(uuid)
                .orElse(null);
                //? }

        if(profile != null) return new LinkerOfflinePlayer(profile.getId().toString(), profile.getName());

        //? if <1.21 {
        profile = server.getSessionService().fillProfileProperties(new GameProfile(uuid, null), false);
        if(profile == null || profile.getName() == null) return null;
        //? } else {
        /*ProfileResult result = server.getSessionService().fetchProfile(uuid, false);
        if(result != null) profile = result.profile();
        else return null;
        *///? }
        return new LinkerOfflinePlayer(profile.getId().toString(), profile.getName());
        //? } else {
        /*String name = server.services().nameToIdCache().get(uuid)
                .map(NameAndId::name)
                .orElse(null);
        if(name != null) return new LinkerOfflinePlayer(uuid.toString(), name);
        return null;
        *///? }
    }


    @Override
    public void broadcastMessage(String message) {
        //? if <1.19 {
        /*server.getPlayerList().broadcastMessage(new TextComponent(message), ChatType.CHAT, Util.NIL_UUID);
        *///?} else
        server.getPlayerList().broadcastSystemMessage(Component.literal(message), false);
    }

    @Override
    public void broadcastMessageWithClickableURLs(String message) {
        Component component = buildURLComponent(message);
        //? if <1.19 {
        /*server.getPlayerList().broadcastMessage(component, ChatType.CHAT, Util.NIL_UUID);
        *///?} else
        server.getPlayerList().broadcastSystemMessage(component, false);
    }

    @Override
    public boolean isPluginOrModEnabled(String pluginNameOrModId) {
        return Platform.isModLoaded(pluginNameOrModId);
    }

    @Override
    public boolean isOnline() {
        return server.usesAuthentication();
    }

    @Override
    public String getMinecraftVersion() {
        return Platform.getMinecraftVersion();
    }

    @Override
    public String getWorldPath() {
        return server.getWorldPath(LevelResource.ROOT).toAbsolutePath().toString();
    }

    @Override
    public String getWorldContainerPath() {
        //? if <1.21 {
        return server.getServerDirectory().getAbsolutePath();
         //? } else
        //return server.getServerDirectory().toAbsolutePath().toString();
    }

    @Override
    public @Nullable String getFloodgatePrefix() {
        //TODO test
        //Load yaml file
        File floodgateConfig = Platform.getConfigFolder().resolve("floodgate").resolve("config.yml").toFile();
        if(!floodgateConfig.exists()) return null;

        try {
            MappingNode config = YamlUtil.load(floodgateConfig.toPath());
            return YamlUtil.getString("username-prefix", config);
        }
        catch(IOException e) {
            getLogger().error("Failed to load Floodgate config: " + e.getMessage());
            return null;
        }
    }

    @Override
    public CompletableFuture<String> executeCommand(String command) {
        CompletableFuture<String> future = new CompletableFuture<>();
        StringBuilder output = new StringBuilder();

        ServerLevel serverLevel = server.overworld();
        CommandSourceStack source = new CommandSourceStack(
                new CommandSource() {

                    //? if >=1.19 {
                    @Override
                    public void sendSystemMessage(Component component) {
                        output.append(componentToFormattedString(component)).append('\n');
                    }

                    //? } else {
                    /*@Override
                    public void sendMessage(@NotNull Component component, @NotNull UUID senderUUID) {
                        output.append(componentToFormattedString(component)).append('\n');
                    }
                    *///? }

                    @Override
                    public boolean acceptsSuccess() {
                        return true;
                    }

                    @Override
                    public boolean acceptsFailure() {
                        return true;
                    }

                    @Override
                    public boolean shouldInformAdmins() {
                        return false;
                    }
                },
                serverLevel == null ? Vec3.ZERO : Vec3.atLowerCornerOf(/*? if <1.21 {*/serverLevel.getSharedSpawnPos() /*? } else { *//*serverLevel.getRespawnData().pos()*//*? }*/),
                Vec2.ZERO,
                serverLevel,
                /*? if <1.21 {*/4/*? } else {*//*PermissionSet.ALL_PERMISSIONS*//*? }*/,
                "Discord",
                //? if <1.19 {
                /*new TextComponent("Discord"),
                *///? } else
                Component.literal("Discord"),
                server,
                null
        ).withCallback((context, success/*? if <1.21 {*/, result /*? }*/) -> {
            String outStr = output.toString().trim();
            if(outStr.isEmpty())
                outStr = success /*? if >=1.21 {*/ /*> 0 *//*? }*/ ? COMMAND_NO_OUTPUT_SUCCESS : COMMAND_NO_OUTPUT_FAIL;
            future.complete(outStr);
        });

        try {
            //? if >=1.19 {
            server.getCommands().performPrefixedCommand(source, command);
             //? } else
            //server.getCommands().performCommand(source, command);
        }
        catch(Exception e) {
            future.complete("Error executing command: " + e.getMessage());
        }

        // If the command was invalid (e.g. unknown command), the ResultConsumer callback
        // is never invoked by Brigadier, so the future would never complete. Complete it
        // here with whatever output was captured (the error message from acceptsFailure).
        if(!future.isDone()) {
            String outStr = output.toString().trim();
            if(outStr.isEmpty()) outStr = COMMAND_NO_OUTPUT_FAIL;
            future.complete(outStr);
        }

        return future;
    }

    /**
     * Converts a Minecraft {@link Component} to a legacy-formatted string
     * with § color/formatting codes, so that colors are preserved for Discord output.
     */
    private static String componentToFormattedString(Component component) {
        StringBuilder sb = new StringBuilder();
        component.visit((style, text) -> {
            TextColor color = style.getColor();

            if(color != null) {
                ChatFormatting legacyColor = chatFormattingFromColor(color);
                if(legacyColor != null) sb.append(legacyColor);
            }
            if(style.isBold()) sb.append(ChatFormatting.BOLD);
            if(style.isItalic()) sb.append(ChatFormatting.ITALIC);
            if(style.isUnderlined()) sb.append(ChatFormatting.UNDERLINE);
            if(style.isStrikethrough()) sb.append(ChatFormatting.STRIKETHROUGH);
            if(style.isObfuscated()) sb.append(ChatFormatting.OBFUSCATED);

            sb.append(text);
            return Optional.empty();
        }, Style.EMPTY);
        return sb.toString();
    }

    /**
     * Maps a {@link TextColor} back to the closest legacy {@link ChatFormatting} color code.
     * Returns null if no legacy color matches (e.g. for custom RGB colors).
     */
    private static @Nullable ChatFormatting chatFormattingFromColor(TextColor color) {
        for(ChatFormatting fmt : ChatFormatting.values()) {
            if(fmt.isColor() && fmt.getColor() != null && fmt.getColor() == color.getValue()) {
                return fmt;
            }
        }
        return null;
    }
}