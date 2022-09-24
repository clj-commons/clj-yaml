(ns build
  (:require [clojure.tools.build.api :as b]))

(def lib 'clj-commons/clj-yaml)
(def version (System/getenv "PROJECT_VERSION"))
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def jar-file (format "target/%s-%s.jar" (name lib) version))

(defn clean [_]
  (b/delete {:path "target"}))

(defn compile-java [_]
  (b/javac {:src-dirs ["src/java"]
            :class-dir class-dir
            :basis basis
            :javac-opts ["-source" "8" "-target" "8"]}))

(defn jar [_]
  (compile-java nil)
  (b/write-pom {:class-dir class-dir
                :lib lib
                :version version
                :basis basis
                :src-dirs ["src/clojure"]})
  (b/copy-dir {:src-dirs ["src/clojure"]
               :target-dir class-dir})
  (b/jar {:class-dir class-dir
          :jar-file jar-file}))

(defn install [_]
  (jar {})
  (b/install {:basis basis
              :lib lib
              :version version
              :jar-file jar-file
              :class-dir class-dir}))

(defn deploy [opts]
  (jar opts)
  ((requiring-resolve 'deps-deploy.deps-deploy/deploy)
   (merge {:installer :remote
           :artifact jar-file
           :pom-file (b/pom-path {:lib lib :class-dir class-dir})}
          opts))
  opts)
