(ns clj-yaml.core
  (:import (org.yaml.snakeyaml Yaml))
  (:use (clojure.contrib [def :only (defvar-)])))

(defvar- yaml (Yaml.))

(defn- stringify [data]
  (cond
    (map? data)
      (into {} (for [[k v] data] [(stringify k) (stringify v)]))
    (coll? data)
      (map stringify data)
    (keyword? data)
      (name data)
    :else
      data))

(defn generate-string [data]
  (.dump yaml (stringify data)))

(defmulti to-seq class)

(defmethod to-seq java.util.LinkedHashMap [data]
  (into {} (for [[k v] data]
                 [(keyword k) (to-seq v)])))

(defmethod to-seq java.util.ArrayList [data]
  (map #(to-seq %) data))

(defmethod to-seq :default [data]
  data)

(defn parse-string [string]
  (to-seq (.load yaml string)))
