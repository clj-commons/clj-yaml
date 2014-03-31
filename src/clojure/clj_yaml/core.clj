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
  (decode [data keywords]))

(defn decode-key [k keywords]
  (if keywords
    ;; (keyword k) is nil for numbers and other values.
    (or (keyword k) k)
    k))

(extend-protocol YAMLCodec
  Marked
  (decode [data keywords]
    ;; Decode the marked data and rewrap it with its source position.
    (Marked. (.start data) (.end data)
             (-> data .marked
                 (decode keywords))))

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
  (decode [data keywords]
     (into {}
           (for [[k v] data]
             [(decode-key k keywords) (decode v keywords)])))

  java.util.LinkedHashSet
  (decode [data keywords]
    (into #{} data))

  java.util.ArrayList
  (decode [data keywords]
    (map #(decode % keywords) data))

  Object
  (encode [data] data)
  (decode [data keywords] data)

  nil
  (encode [data] data)
  (decode [data keywords] data))

(defn generate-string [data & opts]
  (.dump (apply make-yaml opts)
         (encode data)))

(defn parse-string
  [string & {:keys [unsafe mark keywords] :or {keywords true}}]
  (decode (.load (make-yaml :unsafe unsafe :mark mark) string) keywords))
