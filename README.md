`clj-commons/clj-yaml` provides [YAML](http://yaml.org) encoding and
decoding for Clojure via the [snakeyaml][] Java library.

[SnakeYAML]: https://bitbucket.org/asomov/snakeyaml/


[![Clojars Project](https://img.shields.io/clojars/v/clj-commons/clj-yaml.svg)](https://clojars.org/clj-commons/clj-yaml) 
[![cljdoc badge](https://cljdoc.org/badge/clj-commons/clj-yaml)](https://cljdoc.org/d/clj-commons/clj-yaml)
[![CircleCI Status](https://circleci.com/gh/clj-commons/clj-yaml.svg?style=svg)](https://circleci.com/gh/clj-commons/clj-yaml)

(This is a maintained fork of [the original][]).

[the original]: https://github.com/lancepantz/clj-yaml


## Usage

```clojure
(require '[clj-yaml.core :as yaml])

(yaml/generate-string
  [{:name "John Smith", :age 33}
   {:name "Mary Smith", :age 27}])
"- {name: John Smith, age: 33}\n- {name: Mary Smith, age: 27}\n"

(yaml/parse-string "
- {name: John Smith, age: 33}
- name: Mary Smith
  age: 27
")
=> ({:name "John Smith", :age 33}
    {:name "Mary Smith", :age 27})
```

By default, keys are converted to clojure keywords.  To prevent this,
add `:keywords false` parameters to the `parse-string` function:

```clojure
(yaml/parse-string "
- {name: John Smith}
" :keywords false)
```

Different flow styles (`:auto`, `:block`, `:flow`) allow customization of how YAML is rendered:


```clojure
(yaml/generate-string some-data :dumper-options {:flow-style :block})
```

Use the :indent (default: 2) and :indicator-indent (default: 0) options to adjust indentation:

```clojure
(yaml/generate-string some-data :dumper-options {:indent 6
                                                 :indicator-indend 3})
=>
todo:
   -    name: Fix issue
        responsible:
                name: Rita
```
:indent must always be larger than :indicator-indent. If only 1 higher, the indicator will be on a separate line:
```clojure
(yaml/generate-string some-data :dumper-options {:indent 2
                                                 :indicator-indend 1})
=>
todo:
 -
  name: Fix issue
  responsible:
    name: Rita
```

## Installation

`clj-commons/clj-yaml` is available as a Maven artifact from [Clojars](http://clojars.org/clj-commons/clj-yaml).

### Leiningen/Boot

```clojure
[clj-commons/clj-yaml "0.7.0"]
```

### Clojure CLI/`deps.edn`

```clojure
clj-commons/clj-yaml {:mvn/version "0.7.0"}
```

## Development

    $ git clone git://github.com/clj-commons/clj-yaml.git
    $ lein deps
    $ lein test
    $ lein install
