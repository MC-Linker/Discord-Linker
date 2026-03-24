package me.lianecx.discordlinker.architectury.implementation;

import dev.architectury.platform.Platform;
import me.lianecx.discordlinker.common.util.YamlUtil;
import me.lianecx.discordlinker.common.abstraction.core.LinkerConfig;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static me.lianecx.discordlinker.architectury.DiscordLinkerMod.MOD_ID;

public class ModConfig implements LinkerConfig {

    private static final String CONFIG_FILENAME = "config.yml";

    private final Path configFile;

    private MappingNode defaults;
    private MappingNode config;

    public ModConfig(String configFolder) {
        this.configFile = Paths.get(configFolder, CONFIG_FILENAME);

        loadDefaults();
        if(Files.notExists(configFile)) copyDefault();
        load();
    }

    private void loadDefaults() {
        try {
            defaults = YamlUtil.loadResource(getClass().getClassLoader(), CONFIG_FILENAME);
        }
        catch(IOException e) {
            throw new RuntimeException("Failed to load default config", e);
        }
    }

    private void copyDefault() {
        try {
            Files.createDirectories(configFile.getParent());
            Files.copy(getClass().getClassLoader().getResourceAsStream(CONFIG_FILENAME), configFile);
        }
        catch(Exception e) {
            throw new RuntimeException("Failed to copy default config", e);
        }
    }

    private void load() {
        try {
            config = YamlUtil.load(configFile);

            boolean changed = false;
            for(NodeTuple tuple : defaults.getValue()) {
                String key = YamlUtil.getString(((ScalarNode) tuple.getKeyNode()).getValue(), config);
                if(key == null) {
                    YamlUtil.setField(((ScalarNode) tuple.getKeyNode()).getValue(), ((ScalarNode) tuple.getValueNode()).getValue(), config);
                    changed = true;
                }
            }

            if(changed) save();
        }
        catch(IOException e) {
            throw new RuntimeException("Failed to load config", e);
        }
    }

    private void save() {
        try {
            YamlUtil.save(configFile, config);
        }
        catch(IOException e) {
            throw new RuntimeException("Failed to save config", e);
        }
    }

    private @NotNull String getFieldOrDefault(String key) {
        String value = YamlUtil.getString(key, config);
        if(value != null) return value;

        value = YamlUtil.getString(key, defaults);
        if(value != null) return value;

        throw new IllegalStateException("Missing config field: " + key);
    }

    private int getIntOrDefault(String key) {
        try {
            return Integer.parseInt(getFieldOrDefault(key));
        }
        catch(NumberFormatException e) {
            throw new IllegalStateException("Invalid number in config field: " + key);
        }
    }

    @Override
    public boolean isTestVersion() {
        return getPluginVersion().contains("SNAPSHOT");
    }

    @Override
    public String getPluginVersion() {
        return Platform.getMod(MOD_ID).getVersion();
    }

    @Override
    public int getBotPort() {
        return getIntOrDefault("bot_port");
    }

    @Override
    public void setBotPort(int port) {
        YamlUtil.setField("bot_port", port, config);
        save();
    }

    @Override
    public String getTemplateChatMessage() {
        return getFieldOrDefault("message");
    }

    @Override
    public String getTemplatePrivateMessage() {
        return getFieldOrDefault("private_message");
    }

    @Override
    public String getTemplateReplyMessage() {
        return getFieldOrDefault("reply_message");
    }

    @Override
    public int getSyncCheckIntervalSeconds() {
        return getIntOrDefault("sync_check_interval");
    }

    @Override
    public int getChatConsoleFlushSeconds() {
        return getIntOrDefault("chatconsole_flush_seconds");
    }

    @Override
    public int getChatConsoleMaxChars() {
        return getIntOrDefault("chatconsole_max_chars");
    }

    @Override
    public void reload() {
        load();
    }
}
