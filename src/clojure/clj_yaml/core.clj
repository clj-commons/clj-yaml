(ns clj-yaml.core
  (:import (org.yaml.snakeyaml Yaml DumperOptions DumperOptions$FlowStyle)
           (org.yaml.snakeyaml.constructor Constructor SafeConstructor)
           (org.yaml.snakeyaml.representer Representer)
           (clj_yaml Marked MarkedConstructor)))

(def flow-styles
  {:auto DumperOptions$FlowStyle/AUTO
   :block DumperOptions$FlowStyle/BLOCK
   :flow DumperOptions$FlowStyle/FLOW})

(defn make-dumper-options
  [& {:keys [flow-style]}]
  (doto (DumperOptions.)
    (.setDefaultFlowStyle (flow-styles flow-style))))

(defn make-yaml
  [& {:keys [dumper-options unsafe mark]}]
  (let [constructor
        (if unsafe (Constructor.)
            (if mark (MarkedConstructor.) (SafeConstructor.)))
        ;; TODO: unsafe marked constructor
        dumper (if dumper-options 
                 (make-dumper-options :flow-style (:flow-style dumper-options))
                 (DumperOptions.))]
    (Yaml. constructor (Representer.) dumper)))

(defprotocol YAMLCodec
  (encode [data])
  (decode [data keywordize]))

(defn decode-key [k keywordize]
  (if keywordize
    ;; (keyword k) is nil for numbers and other values.
    (or (keyword k) k)
    k))

(extend-protocol YAMLCodec
  Marked
  (decode [data keywordize]
    ;; Decode the marked data and rewrap it with its source position.
    (Marked. (.start data) (.end data)
             (-> data .marked
                 (decode keywordize))))

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
