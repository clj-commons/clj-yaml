(defproject org.clojars.clumsyjedi/clj-yaml (or (System/getenv "PROJECT_VERSION") "0.7.4")
  :description "YAML encoding and decoding for Clojure using SnakeYAML"
  :url "https://github.com/clj-commons/clj-yaml"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "Same as Clojure"}
  ;; Emit warnings on all reflection calls.
  :deploy-repositories [["clojars" {:url "https://repo.clojars.org"
                                    :username :env/clojars_username
                                    :password :env/clojars_password
                                    :sign-releases true}]]

  :global-vars {*warn-on-reflection* true}
  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"]
  :javac-options ["-target" "1.7" "-source" "1.7" "-Xlint:-options"]
  :dependencies
  [[org.yaml/snakeyaml "1.26"]
   [org.flatland/ordered "1.5.9"]]
  :profiles {:provided {:dependencies [[org.clojure/clojure "1.10.1"]]}})
