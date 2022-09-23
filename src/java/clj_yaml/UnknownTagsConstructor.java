package clj_yaml;

import org.yaml.snakeyaml.constructor.Construct;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;

public class UnknownTagsConstructor extends SafeConstructor {

    public UnknownTagsConstructor() {
        this.yamlMultiConstructors.put("", new UnknownTagConstruct());
    }

    public class UnknownTagConstruct extends AbstractConstruct {

        public Object construct(Node node) {
            Tag unknownTag = node.getTag();

            Tag newTag = null;
            switch (node.getNodeId()) {
            case scalar:
                newTag = Tag.STR;
                break;
            case sequence:
                newTag = Tag.SEQ;
                break;
            default:
                newTag = Tag.MAP;
                break;
            }
            node.setTag(newTag);

            return new UnknownTag(unknownTag, getConstructor(node).construct(node));
        }
    }

    public static class UnknownTag {
        public Tag tag;
        public Object value;

        public UnknownTag(Tag unknownTag, Object taggedValue) {
            this.tag = unknownTag;
            this.value = taggedValue;
        }
    }
}
