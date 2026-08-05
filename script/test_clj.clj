(ns test-clj
  (:require [compile-java]
            [helper.clojure-versions :as clojure-versions]
            [helper.jdk :as jdk]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(def cli-clojure-versions (conj (mapv :version (clojure-versions/all)) "all"))

(defn task
  {:org.babashka/cli {:spec (merge (clojure-versions/cli-opt cli-clojure-versions)
                                   {:namepace {:alias :n :coerce :symbol :desc "namespace(s) to test"}
                                    :var {:coerce :symbol :desc "var(s) to test"}})}}
  [{:keys [clojure-version] :as opts}]
  (compile-java/task {})
  (let [env-jdk-version (jdk/version)
        clojure-versions (if (= "all" clojure-version)
                           (clojure-versions/all)
                           [(clojure-versions/lookup clojure-version)])
        test-runner-args (reduce (fn [acc [k v]]
                                   (conj acc (str "--" (name k)) v))
                                 []
                                 (dissoc opts :clojure-version))]
    (doseq [v clojure-versions]
      (if (and (= "all" clojure-version)
               (< (:major env-jdk-version) (:min-jdk-major v)))
        (status/line :warn "Skipping testing clojure version %s\nIt requires min JDK %s, found JDK %s"
                     (:mvn-version v) (:min-jdk-major v) (:version env-jdk-version))
        (do
          (status/line :head "Testing against Clojure version %s" (:mvn-version v))
          (apply shell/clojure (format "-M:%s:test" (:alias v)) test-runner-args))))))
