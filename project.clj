(defproject circleci/clj-yaml "0.5.0"
  :description "YAML encoding and decoding for Clojure using SnakeYAML"
  :url "https://github.com/circleci/clj-yaml"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.yaml/snakeyaml "1.13"]]
  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"])
