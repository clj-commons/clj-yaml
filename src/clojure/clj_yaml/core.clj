(ns clj-yaml.core
  (:require [flatland.ordered.map :refer (ordered-map)]
            [flatland.ordered.set :refer (ordered-set)])
  (:import (org.yaml.snakeyaml Yaml DumperOptions DumperOptions$FlowStyle)
           (org.yaml.snakeyaml.constructor Constructor SafeConstructor)
           (org.yaml.snakeyaml.representer Representer)
           (clj_yaml MarkedConstructor)
           (java.util LinkedHashMap)))

(def flow-styles
  {:auto DumperOptions$FlowStyle/AUTO
   :block DumperOptions$FlowStyle/BLOCK
   :flow DumperOptions$FlowStyle/FLOW})

(defn make-dumper-options
  [& {:keys [flow-style]}]
  (doto (DumperOptions.)
    (.setDefaultFlowStyle (flow-styles flow-style))))

(defn make-yaml
  "Make a yaml encoder/decoder with some given options."
  [& {:keys [dumper-options unsafe mark]}]
  (let [constructor
        (if unsafe (Constructor.)
            (if mark (MarkedConstructor.) (SafeConstructor.)))
        ;; TODO: unsafe marked constructor
        dumper (if dumper-options
                 (make-dumper-options :flow-style (:flow-style dumper-options))
                 (DumperOptions.))]
    (Yaml. constructor (Representer.) dumper)))

(defrecord Marked
  [start end unmark])

(defn mark
  "Mark some data with start and end positions."
  [start end marked]
  (Marked. start end marked))

(defn marked?
  "Let us know whether this piece of data is marked with source positions."
  [m]
  (instance? Marked m))

(defn unmark
  "Strip the source information from this piece of data, if it exists."
  [m]
  (if (marked? m)
    (:unmark m)
    m))

(defprotocol YAMLCodec
  "A protocol for things that can be coerced to and from the types
   that snakeyaml knows how to encode and decode."
  (encode [data])
  (decode [data keywords]))

(extend-protocol YAMLCodec
  clj_yaml.MarkedConstructor$Marked
  (decode [data keywords]
    (letfn [(from-Mark [mark]
              {:line (.getLine mark)
               :index (.getIndex mark)
               :column (.getColumn mark)})]
      ;; Decode the marked data and rewrap it with its source position.
      (mark (-> data .start from-Mark)
            (-> data .end from-Mark)
            (-> data .marked
                (decode keywords)))))

  clojure.lang.IPersistentMap
  (encode [data]
    (let [lhm (LinkedHashMap.)]
      (doseq [[k v] data]
        (.put lhm (encode k) (encode v)))
      lhm))

  clojure.lang.IPersistentCollection
  (encode [data]
    (map encode data))

  clojure.lang.Keyword
  (encode [data]
    (name data))

  java.util.LinkedHashMap
  (decode [data keywords]
    (letfn [(decode-key [k]
              (if keywords
                ;; (keyword k) is nil for numbers etc
                (or (keyword k) k)
                k))]
      (into (ordered-map)
            (for [[k v] data]
              [(-> k (decode keywords) decode-key) (decode v keywords)]))))

  java.util.LinkedHashSet
  (decode [data keywords]
    (into (ordered-set) data))

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
