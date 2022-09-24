(ns clj-yaml.core
  (:require [flatland.ordered.map :refer (ordered-map)]
            [flatland.ordered.set :refer (ordered-set)])
  (:import (org.yaml.snakeyaml Yaml DumperOptions DumperOptions$FlowStyle LoaderOptions)
           (org.yaml.snakeyaml.constructor Constructor SafeConstructor BaseConstructor)
           (org.yaml.snakeyaml.representer Representer)
           (org.yaml.snakeyaml.error Mark)
           (clj_yaml MarkedConstructor UnknownTagsConstructor)
           (java.util LinkedHashMap)
           (clj_yaml MarkedConstructor)
           (java.util LinkedHashMap)
           (java.io StringReader)))

(set! *warn-on-reflection* true)

(def flow-styles
  {:auto DumperOptions$FlowStyle/AUTO
   :block DumperOptions$FlowStyle/BLOCK
   :flow DumperOptions$FlowStyle/FLOW})

(defn default-dumper-options
  "clj-yaml 0.5.6 used SnakeYAML 1.13 which by default did *not* split long
  lines. clj-yaml 0.6.0 upgraded to SnakeYAML 1.23 which by default *did* split
  long lines. This ensures that generate-string uses the older behavior by
  default, for the sake of stability, i.e. backwards compatibility."
  ^DumperOptions []
  (doto (DumperOptions.)
    (.setSplitLines false)))

(defn make-dumper-options
  ^DumperOptions [{:keys [flow-style indent indicator-indent]}]
  (let [dumper (default-dumper-options)]
    (when flow-style
      (.setDefaultFlowStyle dumper (flow-styles flow-style)))
    (when indent
      (.setIndent dumper indent))
    (when indicator-indent
      (.setIndicatorIndent dumper indicator-indent))
    dumper))

(defn default-loader-options
  ^LoaderOptions []
  (LoaderOptions.))

(defn make-loader-options
  ^LoaderOptions [& {:keys [max-aliases-for-collections allow-recursive-keys allow-duplicate-keys]}]
  (let [loader (default-loader-options)]
    (when max-aliases-for-collections
      (.setMaxAliasesForCollections loader max-aliases-for-collections))
    (when allow-recursive-keys
      (.setAllowRecursiveKeys loader allow-recursive-keys))
    (when (instance? Boolean allow-duplicate-keys)
      (.setAllowDuplicateKeys loader allow-duplicate-keys))
    loader))

(defn make-yaml
  "Make a yaml encoder/decoder with some given options."
  ^Yaml [& {:keys [unknown-tag-fn dumper-options unsafe mark max-aliases-for-collections allow-recursive-keys allow-duplicate-keys]}]
  (let [loader (make-loader-options :max-aliases-for-collections max-aliases-for-collections
                                    :allow-recursive-keys allow-recursive-keys
                                    :allow-duplicate-keys allow-duplicate-keys)
        ^BaseConstructor constructor
        (cond
          unsafe (Constructor. loader)

          ;; construct2ndStep isn't implemented by MarkedConstructor,
          ;; causing an exception to be thrown before loader options
          ;; are used
          mark (MarkedConstructor.)

          unknown-tag-fn (UnknownTagsConstructor.)

          ;; TODO: unsafe marked constructor
          :else (SafeConstructor. loader))

        dumper (make-dumper-options dumper-options)]
    (Yaml. constructor (Representer.) dumper loader)))

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
  (decode [data keywords unknown-tag-fn]))

(extend-protocol YAMLCodec
  clj_yaml.MarkedConstructor$Marked
  (decode [data keywords unknown-tag-fn]
    (letfn [(from-Mark [^Mark mark]
              {:line (.getLine mark)
               :index (.getIndex mark)
               :column (.getColumn mark)})]
      ;; Decode the marked data and rewrap it with its source position.
      (mark (-> data .start from-Mark)
            (-> data .end from-Mark)
            (-> data .marked
                (decode keywords unknown-tag-fn)))))

  clj_yaml.UnknownTagsConstructor$UnknownTag
  (decode [data keywords unknown-tag-fn]
    (unknown-tag-fn {:tag (str (.tag data))
                     :value (-> (.value data) (decode keywords unknown-tag-fn))}))

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
    ;; using clojure.core/name would drop the namespace
    (subs (str data) 1))

  java.util.LinkedHashMap
  (decode [data keywords unknown-tag-fn]
    (letfn [(decode-key [k]
              (if keywords
                ;; (keyword k) is nil for numbers etc
                (or (keyword k) k)
                k))]
      (into (ordered-map)
            (for [[k v] data]
              [(-> k (decode keywords unknown-tag-fn) decode-key) (decode v keywords unknown-tag-fn)]))))

  java.util.LinkedHashSet
  (decode [data _keywords _unknown-tag-fn]
    (into (ordered-set) data))

  java.util.ArrayList
  (decode [data keywords unknown-tag-fn]
    (map #(decode % keywords unknown-tag-fn) data))

  Object
  (encode [data] data)
  (decode [data _keywords _unknown-tag-fn] data)

  nil
  (encode [data] data)
  (decode [data _keywords _unknown-tag-fn] data))


(defn generate-string [data & opts]
  (.dump ^Yaml (apply make-yaml opts)
         (encode data)))

(defn- load-stream [^Yaml yaml ^java.io.Reader input load-all keywords unknown-tag-fn]
  (if load-all
    (map #(decode % keywords unknown-tag-fn) (.loadAll yaml input))
    (decode (.load yaml input) keywords unknown-tag-fn)))

(defn parse-string
  [^String string & {:keys [unknown-tag-fn unsafe mark keywords max-aliases-for-collections
                            allow-recursive-keys allow-duplicate-keys load-all] :or {keywords true}}]
  (let [yaml (make-yaml :unsafe unsafe
                        :mark mark
                        :unknown-tag-fn unknown-tag-fn
                        :max-aliases-for-collections max-aliases-for-collections
                        :allow-recursive-keys allow-recursive-keys
                        :allow-duplicate-keys allow-duplicate-keys)]
    (load-stream yaml (StringReader. string) load-all keywords unknown-tag-fn)))

;; From https://github.com/metosin/muuntaja/pull/94/files
(defn generate-stream
  "Dump the content of data as yaml into writer."
  [writer data & opts]
  (.dump ^Yaml (apply make-yaml opts) (encode data) writer))

(defn parse-stream
  [^java.io.Reader reader & {:keys [keywords load-all unknown-tag-fn] :or {keywords true} :as opts}]
  (load-stream (apply make-yaml (into [] cat opts))
               reader
               load-all keywords unknown-tag-fn))
