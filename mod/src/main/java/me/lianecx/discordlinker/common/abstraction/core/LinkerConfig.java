package me.lianecx.discordlinker.common.abstraction.core;

public interface LinkerConfig {
    String getPluginVersion();

    int getBotPort();

    void setBotPort(int port);

    String getTemplateChatMessage();

    String getTemplatePrivateMessage();

    String getTemplateReplyMessage();

    boolean shouldDebug();
}
