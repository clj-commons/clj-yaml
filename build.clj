(ns build
  (:require [babashka.fs :as fs]
            [build-shared]
            [clojure.java.shell :as shell]
            [clojure.tools.build.api :as b]))

(def version (build-shared/lib-version))
(def lib (build-shared/lib-artifact-name))

;; build constants
(def class-dir "target/classes")
(def native-test-class-dir "target/native-test-classes") ;; keep this separate
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
    (let [javac-opts ["-Xlint:-options" "-Werror"]]
      (b/javac (cond-> {:src-dirs ["src/java"]
                        :class-dir class-dir
                        :basis basis
                        ;; don't need -source and -target because by default we are compiling with jdk8
                        :javac-opts javac-opts}
                 (> major 8)
                 ;; --release replaces -source and -target opts for > jdk8
                 (update :javac-opts #(conj % "--release" "8")))))))


(defn compile-clj-for-native-test
  "We compile our tests against our local jar."
  [{:keys [clj-version-alias]}]
  (println "compile-clj to:" native-test-class-dir)
  (let [jars (->> (fs/glob "target" "*.jar") (mapv str))]
    (when (not= (count jars) 1)
      (throw (ex-info (format "Expected 1 jar under ./target to compile against, but found: %s"
                              (if (seq jars) jars "none"))
                      {})))
    (let [jar (first jars)
          basis (b/create-basis {:aliases [:native-test :test clj-version-alias]
                                 :extra {:deps {'clj-commons/clj-yaml {:local/root jar}}}})]
      (println "Using jar:" jar)
      ;; share the classpath for native-image to use in test-native bb task
      (spit "target/native-classpath.edn" (pr-str (:classpath-roots basis)))
      (b/compile-clj {:basis basis
                      :class-dir native-test-class-dir
                      :src-dirs ["test"]
                      :ns-compile ['clj-yaml.native-test-runner]}))))

(defn jar [_]
  (compile-java nil)
  (println "jarring version" version)
  (b/write-pom {:class-dir class-dir
                :lib lib
                :version version
                :scm {:tag (build-shared/version->tag version)}
                :basis basis
                :src-dirs ["src/clojure"]})
  (b/copy-dir {:src-dirs ["src/clojure" "resources"]
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
