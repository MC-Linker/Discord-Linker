package me.lianecx.discordlinker.common.util;

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
import java.nio.file.Path;
import java.util.List;

public final class YamlUtil {

    public static final Yaml YAML = createYaml();

    private YamlUtil() {}

    public static Yaml createYaml() {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setProcessComments(true);

        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setProcessComments(true);
        dumperOptions.setIndent(4);

        BaseConstructor constructor = new SafeConstructor(loaderOptions);
        Representer representer = new Representer(dumperOptions);

        return new Yaml(constructor, representer, dumperOptions, loaderOptions);
    }

    public static MappingNode load(Path path) throws IOException {
        try(InputStream in = Files.newInputStream(path)) {
            return (MappingNode) YAML.compose(new InputStreamReader(in));
        }
    }

    public static MappingNode loadResource(ClassLoader loader, String name) throws IOException {
        try(InputStream in = loader.getResourceAsStream(name)) {
            if(in == null) return null;
            return (MappingNode) YAML.compose(new InputStreamReader(in));
        }
    }

    public static void save(Path path, MappingNode node) throws IOException {
        try(Writer writer = Files.newBufferedWriter(path)) {
            YAML.serialize(node, writer);
        }
    }

    public static String getString(String key, MappingNode node) {
        for(NodeTuple tuple : node.getValue()) {
            if(((ScalarNode) tuple.getKeyNode()).getValue().equals(key)) {
                return ((ScalarNode) tuple.getValueNode()).getValue();
            }
        }
        return null;
    }

    public static void setField(String key, Object value, MappingNode node) {
        int index = indexOf(key, node);
        NodeTuple old = index != -1 ? node.getValue().get(index) : null;

        List<CommentLine> blockComments = old != null ? old.getKeyNode().getBlockComments() : null;
        List<CommentLine> inlineComments = old != null ? old.getValueNode().getInLineComments() : null;

        Node valueNode = createValueNode(value);
        if(inlineComments != null) valueNode.setInLineComments(inlineComments);

        if(old != null) node.getValue().remove(old);

        ScalarNode keyNode = new ScalarNode(Tag.STR, key, null, null, DumperOptions.ScalarStyle.PLAIN);
        if(blockComments != null) keyNode.setBlockComments(blockComments);

        node.getValue().add(index == -1 ? node.getValue().size() : index, new NodeTuple(keyNode, valueNode));
    }

    private static int indexOf(String key, MappingNode node) {
        for(int i = 0; i < node.getValue().size(); i++) {
            NodeTuple tuple = node.getValue().get(i);
            if(((ScalarNode) tuple.getKeyNode()).getValue().equals(key)) {
                return i;
            }
        }
        return -1;
    }

    private static @NotNull Node createValueNode(Object value) {
        if(value instanceof String) {
            return new ScalarNode(Tag.STR, (String) value, null, null, DumperOptions.ScalarStyle.DOUBLE_QUOTED);
        }
        if(value instanceof Integer || value instanceof Boolean) {
            return new ScalarNode(Tag.STR, value.toString(), null, null, DumperOptions.ScalarStyle.PLAIN);
        }
        throw new IllegalArgumentException("Unsupported value type: " + value.getClass());
    }
}
