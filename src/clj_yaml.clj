(ns clj-yaml
  (:import (org.yaml.snakeyaml Yaml))
  (:use (clojure.contrib [def :only (defvar-)])))

(defvar- yaml (Yaml.))

(defmulti to-seq class)

(defmethod to-seq java.util.LinkedHashMap [data] 
  (into {} (for [[k v] data]
                 [k (to-seq v)])))

(defmethod to-seq java.util.ArrayList [data] 
  (map #(to-seq %) data))

(defmethod to-seq :default [data] 
  data)

(defn parse-string [string]
  (to-seq (.load yaml string)))
