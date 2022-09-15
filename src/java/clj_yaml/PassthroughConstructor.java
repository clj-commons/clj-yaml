package clj_yaml;

import org.yaml.snakeyaml.constructor.Construct;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.error.Mark;

/**
 * Implementation of Constructor that ignores YAML tags.
 *
 * This is used as a fallback strategies to use the underlying type instead of
 * throwing an exception.
 */
public class PassthroughConstructor extends Constructor {

    private class PassthroughConstruct extends AbstractConstruct {
        public Object construct(Node node) {
            // reset node to scalar tag type for parsing
            Tag tag = null;
            switch (node.getNodeId()) {
            case scalar:
                tag = Tag.STR;
                break;
            case sequence:
                tag = Tag.SEQ;
                break;
            default:
                tag = Tag.MAP;
                break;
            }

            node.setTag(tag);
            return getConstructor(node).construct(node);
        }

        public void construct2ndStep(Node node, Object object) {}
    }

    public PassthroughConstructor() {
        // Add a catch-all to catch any unidentifiable nodes
        this.yamlMultiConstructors.put("", new PassthroughConstruct());
    }
}
