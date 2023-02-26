package me.lianecx.discordlinker;

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
    public void sendMessage(String message) {
        wrappedSender.sendMessage(message);
        addLog(message);
    }

    @Override
    public void sendMessage(String[] messages) {
        wrappedSender.sendMessage(messages);
        for(String message : messages) {
            addLog(message);
        }
    }

    @Override
    public Server getServer() {
        return wrappedSender.getServer();
    }

    @Override
    public String getName() {
        return "Discord-Linker";
    }

    @Override
    public CommandSender.Spigot spigot() {
        return spigotWrapper;
    }

    @Override
    public boolean isConversing() {
        return wrappedSender.isConversing();
    }

    @Override
    public void acceptConversationInput(String input) {
        wrappedSender.acceptConversationInput(input);
    }

    @Override
    public boolean beginConversation(Conversation conversation) {
        return wrappedSender.beginConversation(conversation);
    }

    @Override
    public void abandonConversation(Conversation conversation) {
        wrappedSender.abandonConversation(conversation);
    }

    @Override
    public void abandonConversation(Conversation conversation, ConversationAbandonedEvent details) {
        wrappedSender.abandonConversation(conversation, details);
    }

    @Override
    public void sendRawMessage(String message) {
        wrappedSender.sendRawMessage(message);
        addLog(message);
    }

    @Override
    public boolean isPermissionSet(String name) {
        return wrappedSender.isPermissionSet(name);
    }

    @Override
    public boolean isPermissionSet(Permission perm) {
        return wrappedSender.isPermissionSet(perm);
    }

    @Override
    public boolean hasPermission(String name) {
        return wrappedSender.hasPermission(name);
    }

    @Override
    public boolean hasPermission(Permission perm) {
        return wrappedSender.hasPermission(perm);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value) {
        return wrappedSender.addAttachment(plugin, name, value);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin) {
        return wrappedSender.addAttachment(plugin);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value, int ticks) {
        return wrappedSender.addAttachment(plugin, name, value, ticks);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, int ticks) {
        return wrappedSender.addAttachment(plugin, ticks);
    }

    @Override
    public void removeAttachment(PermissionAttachment attachment) {
        wrappedSender.removeAttachment(attachment);
    }

    @Override
    public void recalculatePermissions() {
        wrappedSender.recalculatePermissions();
    }

    @Override
    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
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
        public void sendMessage(BaseComponent component) {
            wrappedSender.spigot().sendMessage();
            addLog(BaseComponent.toLegacyText(component));
        }

        /**
         * Sends an array of components as a single message to the sender.
         *
         * @param components the components to send
         */
        public void sendMessage(BaseComponent... components) {
            wrappedSender.spigot().sendMessage(components);
            addLog(BaseComponent.toLegacyText(components));
        }
    }
}