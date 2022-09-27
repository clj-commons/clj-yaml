(ns build
  (:require [build-shared]
            [clojure.java.shell :as shell]
            [clojure.tools.build.api :as b]))

(def version (build-shared/lib-version))
(def lib (build-shared/lib-artifact-name))

;; build constants
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def jar-file (format "target/%s-%s.jar" (name lib) version))

(defn- jdk-version
  "Returns jdk version and major version with appropriate conversion. (ex 1.8 returns major of 8)"
  []
  (let [raw-version (->> (shell/sh "java" "-version")
                         :err
                         (re-find #"version \"(.*)\"")
                         last)
        major-minor (->> raw-version
                         (re-find #"(\d+)(?:\.(\d+))?.*")
                         rest
                         (map #(when % (Integer/parseInt %))))]
    {:version raw-version
     :major (if (= (first major-minor) 1)
              (second major-minor)
              (first major-minor))}))

(defn compile-java [_]
  (let [{:keys [version major]} (jdk-version)]
    (println "compile-java with java version" version)
    (when (< major 8)
      (throw (ex-info "jdk version must be at least 8" {})))
    (b/javac (cond-> {:src-dirs ["src/java"]
                      :class-dir class-dir
                      :basis basis}
               (> major 8)
               ;; replaces old jdk <= 8 -source and -target opts
               (assoc :javac-opts ["--release" "8" "-Xlint"])))))

(defn jar [_]
  (compile-java nil)
  (println "jarring version" version)
  (b/write-pom {:class-dir class-dir
                :lib lib
                :version version
                :scm {:tag (build-shared/version->tag version)}
                :basis basis
                :src-dirs ["src/clojure"]})
  (b/copy-dir {:src-dirs ["src/clojure"]
               :target-dir class-dir})
  (b/jar {:class-dir class-dir
          :jar-file jar-file}))

(defn install [_]
  (jar {})
  (println "installing version" version)
  (b/install {:basis basis
              :lib lib
              :version version ;; can't remember why we need to repeat version here, it is in jar-file
              :jar-file jar-file
              :class-dir class-dir}))

(defn deploy [opts]
  (jar opts)
  (println "deploy")
  ((requiring-resolve 'deps-deploy.deps-deploy/deploy)
   (merge {:installer :remote
           :artifact jar-file
           :pom-file (b/pom-path {:lib lib :class-dir class-dir})}
          opts))
  opts)
