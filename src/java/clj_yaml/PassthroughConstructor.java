// MIT License

// Copyright (c) 2016 Owain Lewis

// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:

// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.

// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

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
