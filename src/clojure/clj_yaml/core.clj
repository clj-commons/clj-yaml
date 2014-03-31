(ns clj-yaml.core
  (:import (org.yaml.snakeyaml Yaml DumperOptions DumperOptions$FlowStyle)
           (org.yaml.snakeyaml.constructor Constructor SafeConstructor)
           (org.yaml.snakeyaml.representer Representer)
           (clj_yaml MarkedConstructor)))

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

(defrecord Marked
  [start end unmark])

(defn mark [start end marked]
  (Marked. start end marked))

(defn marked? [m]
  (instance? Marked m))

(defn unmark [m]
  (if (marked? m)
    (:unmark m)
    m))

(defprotocol YAMLCodec
  (encode [data])
  (decode [data keywords]))

(defn decode-key [k keywords]
  (if keywords
    ;; (keyword k) is nil for numbers and other values.
    (or (keyword k) k)
    k))

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
