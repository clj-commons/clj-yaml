(ns clj-yaml.core
  (:import (org.yaml.snakeyaml Yaml DumperOptions DumperOptions$FlowStyle)
           (org.yaml.snakeyaml.constructor Constructor SafeConstructor)
           (org.yaml.snakeyaml.representer Representer)))

(def ^{:dynamic true} *keywordize* true)

(def flow-styles
  {:auto DumperOptions$FlowStyle/AUTO
   :block DumperOptions$FlowStyle/BLOCK
   :flow DumperOptions$FlowStyle/FLOW})

(defn make-dumper-options
  [& {:keys [flow-style]}]
  (doto (DumperOptions.)
    (.setDefaultFlowStyle (flow-styles flow-style))))

(defn make-yaml
  [& {:keys [dumper-options unsafe]}]
  (let [constructor (if unsafe (Constructor.) (SafeConstructor.))
        dumper (if dumper-options 
                 (make-dumper-options :flow-style (:flow-style dumper-options))
                 (DumperOptions.))]
    (Yaml. constructor (Representer.) dumper)))

(defprotocol YAMLCodec
  (encode [data])
  (decode [data]))

(defn decode-key [k]
  (if *keywordize* (keyword k) k))

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
  (decode [data]
    (into {}
          (for [[k v] data]
            [(decode-key k) (decode v)])))

  java.util.LinkedHashSet
  (decode [data]
    (into #{} data))

  java.util.ArrayList
  (decode [data]
    (map decode data))

  Object
  (encode [data] data)
  (decode [data] data)

  nil
  (encode [data] data)
  (decode [data] data))

(defn generate-string [data & opts]
  (.dump (apply make-yaml opts)
         (encode data)))

(defn parse-string
  ([string keywordize]
     (binding [*keywordize* keywordize]
       (parse-string string)))
  ([string]
     (decode (.load (make-yaml) string))))
