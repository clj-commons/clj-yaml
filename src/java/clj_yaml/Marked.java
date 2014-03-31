package clj_yaml;

import org.yaml.snakeyaml.error.Mark;

public class Marked {
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
