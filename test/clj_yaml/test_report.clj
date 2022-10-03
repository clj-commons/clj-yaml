(ns clj-yaml.test-report
  (:require [clojure.test]))

(def platform
  (str "jvm-clj " (clojure-version)))

(defmethod clojure.test/report :begin-test-var [m]
  (let [test-name (-> m :var meta :name)]
    (println (format "=== %s [%s]" test-name platform))))
