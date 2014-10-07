`circleci/clj-yaml` provides [YAML](http://yaml.org) encoding and
decoding for Clojure via the [snakeyaml][] Java library.

[SnakeYAML]: http://code.google.com/p/snakeyaml/

![CircleCI Status][]

[CircleCI Status]: https://circleci.com/gh/circleci/clj-yaml.png

(This is a maintained fork of [the original][]).

[the original]: https://github.com/lancepantz/clj-yaml


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

`circleci/clj-yaml` is available as a Maven artifact from [Clojars][]:

[Clojars]: http://clojars.org/circleci/clj-yaml):

    :dependencies [[circleci/clj-yaml "0.5.3"]  ...]

## Development

    $ git clone git://github.com/circleci/clj-yaml.git
    $ lein deps
    $ lein test
    $ lein install
