(ns clj-yaml.core
  (:import (org.yaml.snakeyaml Yaml DumperOptions DumperOptions$FlowStyle)))

(def flow-styles
  {:auto DumperOptions$FlowStyle/AUTO
   :block DumperOptions$FlowStyle/BLOCK
   :flow DumperOptions$FlowStyle/FLOW})

(defn make-dumper-options
  [& {:keys [flow-style]}]
  (doto (DumperOptions.)
    (.setDefaultFlowStyle (flow-styles flow-style))))

(defn make-yaml
  [& {:keys [dumper-options]}]
  (if dumper-options
    (Yaml. (apply make-dumper-options
                  (mapcat (juxt key val)
                          dumper-options)))
    (Yaml.)))

(defprotocol YAMLCodec
  (encode [data])
  (decode [data keywordize]))

(defn decode-key [k keywordize]
  (if keywordize (keyword k) k))

(extend-protocol YAMLCodec

  clojure.lang.IPersistentMap
  (encode [data]
    (into {}
          (for [[k v] data]
            [(encode k) (encode v)])))

  clojure.lang.IPersistentCollection
  (encode [data]
    (map encode data))

  clojure.lang.Keyword
  (encode [data]
    (name data))

  java.util.LinkedHashMap
  (decode [data keywordize]
     (into {}
           (for [[k v] data]
             [(decode-key k keywordize) (decode v keywordize)])))

  java.util.LinkedHashSet
  (decode [data keywordize]
    (into #{} data))

  java.util.ArrayList
  (decode [data keywordize]
    (map #(decode % keywordize) data))

  Object
  (encode [data] data)
  (decode [data keywordize] data)

  nil
  (encode [data] data)
  (decode [data keywordize] data))

(defn generate-string [data & opts]
  (.dump (apply make-yaml opts)
         (encode data)))

(defn parse-string
  ([string keywordize]
     (decode (.load (make-yaml) string) keywordize))
  ([string]
     (parse-string string true)))
