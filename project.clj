(defproject clj-yaml "0.2.0-SNAPSHOT"
              :description      "YAML encoding(eventually) and decoding for Clojure using SnakeYAML"
              :url              "http://github.com/lancepantz/clj-yaml"
              :source-path      "src"
              :dependencies     [[org.clojure/clojure "1.1.0"]
                                 [org.clojure/clojure-contrib "1.1.0"]
                                 [org.yaml/snakeyaml "1.5"]]
              :repositories     [["snakeyaml" "http://snakeyamlrepo.appspot.com/repository"]]
              :dev-dependencies [[lein-clojars "0.5.0-SNAPSHOT"]])
