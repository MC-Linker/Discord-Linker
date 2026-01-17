package me.lianecx.discordlinker.architectury.implementation;

import dev.architectury.platform.Platform;
import me.lianecx.discordlinker.common.abstraction.core.LinkerConfig;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.comments.CommentLine;
import org.yaml.snakeyaml.constructor.BaseConstructor;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.nodes.*;
import org.yaml.snakeyaml.representer.Representer;

import java.io.*;
import java.nio.file.Files;
import java.util.List;

import static me.lianecx.discordlinker.architectury.DiscordLinkerArchitectury.MOD_ID;

public class ArchitecturyConfig implements LinkerConfig {

    private static final String CONFIG_FILENAME = "linker.yml";
    private static final String DEFAULT_CONFIG_FILENAME = "config.yml";

    private final Yaml YAML;
    private final File configFile;

    private MappingNode defaults;
    private MappingNode config;

    public ArchitecturyConfig(String configFolder) {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setProcessComments(true);
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setProcessComments(true);

        BaseConstructor constructor = new SafeConstructor(loaderOptions);
        Representer representer = new Representer(dumperOptions);

        YAML = new Yaml(constructor, representer, dumperOptions, loaderOptions);

        this.configFile = new File(new File(configFolder), CONFIG_FILENAME);

        loadDefaults();
        if(!configFile.exists()) copyDefault();
        load();
    }

    private void loadDefaults() {
        try(InputStream in = getClass().getClassLoader().getResourceAsStream(DEFAULT_CONFIG_FILENAME)) {
            if(in != null) this.defaults = (MappingNode) YAML.compose(new InputStreamReader(in));
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    private void copyDefault() {
        try(InputStream in = getClass().getClassLoader().getResourceAsStream(DEFAULT_CONFIG_FILENAME)) {
            if(in != null) Files.copy(in, configFile.toPath());
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    public void load() {
        try(InputStream in = Files.newInputStream(configFile.toPath())) {
            this.config = (MappingNode) YAML.compose(new InputStreamReader(in));

            boolean changed = false;
            for(NodeTuple tuple : defaults.getValue()) {
                String key = ((ScalarNode) tuple.getKeyNode()).getValue();
                if(getFieldFromMapping(key, config) == null) {
                    setField(key, ((ScalarNode) tuple.getValueNode()).getValue());
                    changed = true;
                }
            }

            if(changed) save();
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    public void save() {
        try(Writer writer = new FileWriter(configFile)) {
            YAML.serialize(config, writer);
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    private String getFieldFromMapping(String key, MappingNode node) {
        for(NodeTuple tuple : node.getValue()) {
            if(((ScalarNode) tuple.getKeyNode()).getValue().equals(key))
                return ((ScalarNode) tuple.getValueNode()).getValue();
        }
        return null;
    }

    private @NotNull String getFieldOrDefault(String key) {
        String value = getFieldFromMapping(key, config);
        if(value != null) return value;

        value = getFieldFromMapping(key, defaults);
        if(value != null) return value;

        throw new IllegalStateException("Missing config field: " + key);
    }

    private void setField(String key, Object value) {
        int index = getNodeTupleIndex(key);
        NodeTuple oldTuple = index != -1 ? config.getValue().get(index) : null;

        List<CommentLine> blockComments = oldTuple != null ? oldTuple.getKeyNode().getBlockComments() : null;
        List<CommentLine> inlineComments = oldTuple != null ? oldTuple.getValueNode().getInLineComments() : null;

        Node valueNode;
        if(value instanceof String)
            valueNode = new ScalarNode(Tag.STR, (String) value, null, null, DumperOptions.ScalarStyle.DOUBLE_QUOTED);
        else if(value instanceof Integer)
            valueNode = new ScalarNode(Tag.INT, value.toString(), null, null, DumperOptions.ScalarStyle.PLAIN);
        else if(value instanceof Boolean)
            valueNode = new ScalarNode(Tag.BOOL, value.toString(), null, null, DumperOptions.ScalarStyle.PLAIN);
        else
            throw new IllegalArgumentException("Unsupported value type: " + value.getClass());

        if(inlineComments != null)
            valueNode.setInLineComments(inlineComments);

        if(oldTuple != null)
            config.getValue().remove(oldTuple);

        ScalarNode keyNode = new ScalarNode(Tag.STR, key, null, null, DumperOptions.ScalarStyle.PLAIN);
        if(blockComments != null)
            keyNode.setBlockComments(blockComments);

        config.getValue().add(index == -1 ? config.getValue().size() : index, new NodeTuple(keyNode, valueNode));
    }

    private int getNodeTupleIndex(String key) {
        for(int i = 0; i < config.getValue().size(); i++) {
            NodeTuple tuple = config.getValue().get(i);
            if(((ScalarNode) tuple.getKeyNode()).getValue().equals(key))
                return i;
        }
        return -1;
    }

    private int getIntOrDefault(String key) {
        try {
            return Integer.parseInt(getFieldOrDefault(key));
        }
        catch(NumberFormatException e) {
            throw new IllegalStateException("Invalid number in config field: " + key);
        }
    }

    private boolean getBoolOrDefault(String key) {
        return Boolean.parseBoolean(getFieldOrDefault(key));
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
        setField("bot_port", port); save();
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
    public boolean shouldDebug() {
        return getBoolOrDefault("debug_mode");
    }
}