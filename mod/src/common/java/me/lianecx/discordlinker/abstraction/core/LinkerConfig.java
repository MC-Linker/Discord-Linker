package me.lianecx.discordlinker.abstraction.core;

public interface LinkerConfig {
    String getPluginVersion();

    int getBotPort();

    void setBotPort(int port);

    String getTemplateChatMessage();

    String getTemplatePrivateMessage();

    String getTemplateReplyMessage();

    boolean shouldDebug();
}
