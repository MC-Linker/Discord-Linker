package me.lianecx.discordlinker;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MessageInterceptingCommandSender implements ConsoleCommandSender {

    private final ConsoleCommandSender wrappedSender;
    private final Spigot spigotWrapper;
    private final List<String> loggedData = new ArrayList<>();
    private boolean isLogging = false;

    public MessageInterceptingCommandSender(ConsoleCommandSender wrappedSender) {
        this.wrappedSender = wrappedSender;
        spigotWrapper = new Spigot();
    }

    public List<String> getData() {
        return new ArrayList<>(loggedData);
    }

    public void clearData() {
        loggedData.clear();
    }

    public void addLog(String log) {
        if(isLogging) loggedData.add(log);
    }

    public void startLogging() {
        isLogging = true;
    }

    public void stopLogging() {
        isLogging = false;
    }

    @Override
    public void sendMessage(@NotNull String message) {
        wrappedSender.sendMessage(message);
        addLog(message);
    }

    @Override
    public void sendMessage(@NotNull String[] messages) {
        wrappedSender.sendMessage(messages);
        for(String message : messages) {
            addLog(message);
        }
    }

    @Override
    public @NotNull Server getServer() {
        return wrappedSender.getServer();
    }

    @Override
    public @NotNull String getName() {
        return "Discord-Linker";
    }

    @Override
    public @NotNull CommandSender.Spigot spigot() {
        return spigotWrapper;
    }

    @Override
    public boolean isConversing() {
        return wrappedSender.isConversing();
    }

    @Override
    public void acceptConversationInput(@NotNull String input) {
        wrappedSender.acceptConversationInput(input);
    }

    @Override
    public boolean beginConversation(@NotNull Conversation conversation) {
        return wrappedSender.beginConversation(conversation);
    }

    @Override
    public void abandonConversation(@NotNull Conversation conversation) {
        wrappedSender.abandonConversation(conversation);
    }

    @Override
    public void abandonConversation(@NotNull Conversation conversation, @NotNull ConversationAbandonedEvent details) {
        wrappedSender.abandonConversation(conversation, details);
    }

    @Override
    public void sendRawMessage(@NotNull String message) {
        wrappedSender.sendRawMessage(message);
        addLog(message);
    }

    @Override
    public boolean isPermissionSet(@NotNull String name) {
        return wrappedSender.isPermissionSet(name);
    }

    @Override
    public boolean isPermissionSet(@NotNull Permission perm) {
        return wrappedSender.isPermissionSet(perm);
    }

    @Override
    public boolean hasPermission(@NotNull String name) {
        return wrappedSender.hasPermission(name);
    }

    @Override
    public boolean hasPermission(@NotNull Permission perm) {
        return wrappedSender.hasPermission(perm);
    }

    @Override
    public @NotNull PermissionAttachment addAttachment(@NotNull Plugin plugin, @NotNull String name, boolean value) {
        return wrappedSender.addAttachment(plugin, name, value);
    }

    @Override
    public @NotNull PermissionAttachment addAttachment(@NotNull Plugin plugin) {
        return wrappedSender.addAttachment(plugin);
    }

    @Override
    public @Nullable PermissionAttachment addAttachment(@NotNull Plugin plugin, @NotNull String name, boolean value, int ticks) {
        return wrappedSender.addAttachment(plugin, name, value, ticks);
    }

    @Override
    public @Nullable PermissionAttachment addAttachment(@NotNull Plugin plugin, int ticks) {
        return wrappedSender.addAttachment(plugin, ticks);
    }

    @Override
    public void removeAttachment(@NotNull PermissionAttachment attachment) {
        wrappedSender.removeAttachment(attachment);
    }

    @Override
    public void recalculatePermissions() {
        wrappedSender.recalculatePermissions();
    }

    @Override
    public @NotNull Set<PermissionAttachmentInfo> getEffectivePermissions() {
        return wrappedSender.getEffectivePermissions();
    }

    @Override
    public boolean isOp() {
        return wrappedSender.isOp();
    }

    @Override
    public void setOp(boolean value) {
        wrappedSender.setOp(value);
    }

    private class Spigot extends CommandSender.Spigot {
        /**
         * Sends this sender a chat component.
         *
         * @param component the components to send
         */
        public void sendMessage(@NotNull net.md_5.bungee.api.chat.BaseComponent component) {
            wrappedSender.spigot().sendMessage();
            addLog(BaseComponent.toLegacyText(component));
        }

        /**
         * Sends an array of components as a single message to the sender.
         *
         * @param components the components to send
         */
        public void sendMessage(@NotNull net.md_5.bungee.api.chat.BaseComponent... components) {
            wrappedSender.spigot().sendMessage(components);
            addLog(BaseComponent.toLegacyText(components));
        }
    }
}