package clj_yaml;

import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;
import clj_yaml.Marked;

public class MarkedConstructor extends SafeConstructor {
    /* A subclass of SafeConstructor that wraps all the type-specific
       constructors it defines with versions that mark the start and
       end positions.
    */
    public MarkedConstructor() {
        // Make sure SafeConstructor's constructor is called first,
        // so that we overwrite the keys that SafeConstructor sets.
        super();
        this.yamlConstructors.put(Tag.NULL, new ConstructYamlNull());
        this.yamlConstructors.put(Tag.BOOL, new ConstructYamlBool());
        this.yamlConstructors.put(Tag.INT, new ConstructYamlInt());
        this.yamlConstructors.put(Tag.FLOAT, new ConstructYamlFloat());
        this.yamlConstructors.put(Tag.BINARY, new ConstructYamlBinary());
        this.yamlConstructors.put(Tag.TIMESTAMP, new ConstructYamlTimestamp());
        this.yamlConstructors.put(Tag.OMAP, new ConstructYamlOmap());
        this.yamlConstructors.put(Tag.PAIRS, new ConstructYamlPairs());
        this.yamlConstructors.put(Tag.SET, new ConstructYamlSet());
        this.yamlConstructors.put(Tag.STR, new ConstructYamlStr());
        this.yamlConstructors.put(Tag.SEQ, new ConstructYamlSeq());
        this.yamlConstructors.put(Tag.MAP, new ConstructYamlMap());
        this.yamlConstructors.put(null, undefinedConstructor);
    }
    public class ConstructYamlNull extends SafeConstructor.ConstructYamlNull {
        public Object construct(Node node) {
            return new Marked
                (node.getStartMark(),
                 node.getEndMark(),
                 super.construct(node));
        }
    }
    public class ConstructYamlBool extends SafeConstructor.ConstructYamlBool {
        public Object construct(Node node) {
            return new Marked
                (node.getStartMark(),
                 node.getEndMark(),
                 super.construct(node));
        }
    }
    public class ConstructYamlInt extends SafeConstructor.ConstructYamlInt {
        public Object construct(Node node) {
            return new Marked
                (node.getStartMark(),
                 node.getEndMark(),
                 super.construct(node));
        }
    }
    public class ConstructYamlFloat extends SafeConstructor.ConstructYamlFloat {
        public Object construct(Node node) {
            return new Marked
                (node.getStartMark(),
                 node.getEndMark(),
                 super.construct(node));
        }
    }
    public class ConstructYamlBinary extends SafeConstructor.ConstructYamlBinary {
        public Object construct(Node node) {
            return new Marked
                (node.getStartMark(),
                 node.getEndMark(),
                 super.construct(node));
        }
    }
    public class ConstructYamlTimestamp extends SafeConstructor.ConstructYamlTimestamp {
        public Object construct(Node node) {
            return new Marked
                (node.getStartMark(),
                 node.getEndMark(),
                 super.construct(node));
        }
    }
    public class ConstructYamlOmap extends SafeConstructor.ConstructYamlOmap {
        public Object construct(Node node) {
            return new Marked
                (node.getStartMark(),
                 node.getEndMark(),
                 super.construct(node));
        }
    }
    public class ConstructYamlPairs extends SafeConstructor.ConstructYamlPairs {
        public Object construct(Node node) {
            return new Marked
                (node.getStartMark(),
                 node.getEndMark(),
                 super.construct(node));
        }
    }
    public class ConstructYamlSet extends SafeConstructor.ConstructYamlSet {
        public Object construct(Node node) {
            return new Marked
                (node.getStartMark(),
                 node.getEndMark(),
                 super.construct(node));
        }
    }
    public class ConstructYamlStr extends SafeConstructor.ConstructYamlStr {
        public Object construct(Node node) {
            return new Marked
                (node.getStartMark(),
                 node.getEndMark(),
                 super.construct(node));
        }
    }
    public class ConstructYamlSeq extends SafeConstructor.ConstructYamlSeq {
        public Object construct(Node node) {
            return new Marked
                (node.getStartMark(),
                 node.getEndMark(),
                 super.construct(node));
        }
    }
    public class ConstructYamlMap extends SafeConstructor.ConstructYamlMap {
        public Object construct(Node node) {
            return new Marked
                (node.getStartMark(),
                 node.getEndMark(),
                 super.construct(node));
        }
    }
}
