`circleci/clj-yaml` provides [YAML](http://yaml.org) encoding and
decoding for Clojure via the [snakeyaml][] Java library.

[SnakeYAML]: https://bitbucket.org/asomov/snakeyaml/

[![CircleCI Status](https://circleci.com/gh/circleci/clj-yaml.svg?style=svg)](https://circleci.com/gh/circleci/clj-yaml)

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

By default, keys are converted to clojure keywords.  To prevent this, 
add `:keywords false` parameters to the `parse-string` function:

    (yaml/parse-string "
    - {name: John Smith}
    " :keywords false)

## Installation

`circleci/clj-yaml` is available as a Maven artifact from [Clojars][]:

[Clojars]: http://clojars.org/circleci/clj-yaml):

    :dependencies [[circleci/clj-yaml "0.5.6"]  ...]

## Development

    $ git clone git://github.com/circleci/clj-yaml.git
    $ lein deps
    $ lein test
    $ lein install
