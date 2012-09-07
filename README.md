`clj-yaml` provides [YAML](http://yaml.org) encoding and decoding for Clojure via the [SnakeYAML](http://code.google.com/p/snakeyaml/) Java library.

## Usage

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

## Installation

`clj-yaml` is available as a Maven artifact from [Clojars](http://clojars.org/clj-yaml):

    :dependencies
      [["clj-yaml" "0.4.0"]
       ...]

## Development

    $ git clone git://github.com/lancepantz/clj-yaml.git
    $ lein deps
    $ lein test
    $ lein install
