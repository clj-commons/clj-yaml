package clj_yaml;

import org.yaml.snakeyaml.constructor.Construct;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.error.Mark;

/* A subclass of SafeConstructor that wraps all the type-specific
   constructors it defines with versions that mark the start and
   end positions.
*/
public class MarkedConstructor extends SafeConstructor {
    /* The types we want to wrap. */
    public static Tag[] tags = {Tag.NULL, Tag.BOOL, Tag.INT, Tag.FLOAT,
                                Tag.BINARY, Tag.TIMESTAMP, Tag.OMAP,
                                Tag.PAIRS, Tag.SET, Tag.STR, Tag.SEQ, Tag.MAP};

    public MarkedConstructor() {
        // Make sure SafeConstructor's constructor is called first,
        // so that we overwrite the keys that SafeConstructor sets.
        super();
        // Wrap all the constructors with Marking constructors.
        for (Tag tag : tags) {
            Construct old = this.yamlConstructors.get(tag);
            this.yamlConstructors.put(tag, new Marking(old));
        }
    }
    /* An intermediate representation of data marked with start and
       end positions before we turn it into the nice clojure thing.
    */
    public static class Marked {
        /* An object paired with start and end Marks. */
        public Mark start;
        public Mark end;
        public Object marked;
        public Marked(Mark start, Mark end, Object marked) {
            this.start = start;
            this.end = end;
            this.marked = marked;
        }
    }

    /* A wrapper around a Construct that marks source positions before calling
       the original.
     */
    public class Marking extends AbstractConstruct {
        public Construct constructor;
        public Marking(Construct constructor) {
            this.constructor = constructor;
        }
        public Object construct(Node node) {
            return new Marked
                (node.getStartMark(),
                 node.getEndMark(),
                 constructor.construct(node));
        }
    }
}
