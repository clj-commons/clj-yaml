(ns clj-yaml.core
  "Parse and generate YAML.

  Please strongly prefer these high level functions:
  - [[parse-stream]]
  - [[parse-string]]
  - [[generate-stream]]
  - [[generate-string]]
  - [[unmark]]

  If history were to be rewritten we might have started with the above as
  our public API.

  The clj-commons/clj-yaml team noticed that folks were using other parts of
  this namespace in the wild and therefore continue to support them.

  If you find yourself using something in clj-yaml not listed above, it could be
  you are doing so to overcome a limitation that we could address in clj-yaml itself.
  If that's the case, we encourage you to work with us to potentially improve
  clj-yaml for everybody by raising an issue.

  General note: We don't do any wrapping of SnakeYAML exceptions.
  The SnakeYAML base exception is `org.yaml.snakeyaml.error.YAMLException`."
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
  "[low level, please consider higher level API first]

  Internal mappings to SnakeYAML's internal flow styles"
  {:auto DumperOptions$FlowStyle/AUTO
   :block DumperOptions$FlowStyle/BLOCK
   :flow DumperOptions$FlowStyle/FLOW})

(defn default-dumper-options
  "[low level, please consider higher level API first]

  Returns internal default SnakeYAML dumper options.
  - preserves clj-yaml backward compat by explicitly setting option to split long lines to false.
  The current default in SnakeYAML used to be false but has become true.

  Consider instead [[make-dumper-options]]"
  ^DumperOptions []
  (doto (DumperOptions.)
    (.setSplitLines false)))

(defn make-dumper-options
  "[low level, please consider higher level API first]

  Returns internal SnakeYAML dumper options.
  See [[generate-string]] for description of options."
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
  "[low level, please consider higher level API first]

  Returns internal default SnakeYAML loader options."
  ^LoaderOptions []
  (LoaderOptions.))

(defn make-loader-options
  "[low level, please consider higher level API first]

  Returns internal SnakeYAML loader options.
  See [[parse-string]] for description of options."
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
  "[low level, please consider higher level API first]

  Returns internal SnakeYAML encoder/decoder.

  See [[parse-string]] and [[generate-string]] for description of options."
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
  "[low level, please consider higher level API first]

  Returns internal wrapped `marked` with `start` and `end` positional data."
  [start end marked]
  (Marked. start end marked))

(defn marked?
  "Returns `true` if `m` was marked with positional data.

  See [docs](/README.adoc#mark)."
  [m]
  (instance? Marked m))

(defn unmark
  "Returns `m` without positional data wrapper, else `m` if not wrapped.

  See [docs](/README.adoc#mark)."
  [m]
  (if (marked? m)
    (:unmark m)
    m))

(defprotocol YAMLCodec
  "[low level, please consider higher level API first]

  A protocol to translate to/from Clojure and SnakeYAML constructs"
  (encode [data] "Encode Clojure -> SnakeYAML")
  (decode [data keywords unknown-tag-fn] "Decode SnakeYAML -> Clojure"))

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

(defn generate-string
  "Return a string of YAML from Clojure `data` strucutre.

  Relevant `& opts` (don't wrap `opts` in map):
  - `:dumper-options` map of (see [docs](/README.adoc#dumper-options) for example usage.):
    - `:flow-style` can be:
      - `:auto` - let SnakeYAML decide
      - `:block` - indented syntax
      - `:flow` - collapsed syntax
      - default: `:auto`
    - `:indent` - spaces to block indent
      - default: `2`
    - `:indicator-indent` - spaces to indent after indicator
      - default: `0`"
  [data & opts]
  (.dump ^Yaml (apply make-yaml opts)
         (encode data)))

(defn- load-stream [^Yaml yaml ^java.io.Reader input load-all keywords unknown-tag-fn]
  (if load-all
    (map #(decode % keywords unknown-tag-fn) (.loadAll yaml input))
    (decode (.load yaml input) keywords unknown-tag-fn)))

(defn parse-string
  "Returns parsed `yaml-string` as Clojure structures.

  Valid `& opts` (don't wrap `opts` in map):
  - `:keywords` - when `true` attempts to convert YAML keys to Clojure keywords, else makes no conversion
    - default: `true`.
    - when clj-yaml detects that a YAML key cannot be converted to a legal Clojure keyword it leaves the key as is
    - detection is not sophisticated and clj-yaml will produce invalid Clojure keywords, so although our default is `true` here, `false` can be a better choice.
  - `:load-all` - when `true` loads all YAML documents from `yaml-string` and returns a vector of parsed docs.
  Else only first YAML document is loaded, and return is that individual parsed doc.
    - default: `false`
  - `:unknown-tag-fn` - Single-argument fn, arg is map with keys `:tag` and `:value`; return replaces the YAML tag and value.
    - default behaviour: clj-yaml throws on unknown tags.
    - see [docs](/README.adoc#unknown-tags) for example usage.
  - `:max-aliases-for-collections` the maximum number of YAML aliases for collections (sequences and mappings).
    - Default: `50`
    - throws when value is exceeded.
  - `:allow-recursive-keys` - when `true` allows recursive keys for mappings. Only checks the case where the key is the direct value.
    - Default: `false`
  - `:allow-duplicate-keys` - when `false` throws on duplicate keys.
    - Default: `true` - last duplicate key wins.
  - `:unsafe` - when `true` attempt to load tags to custom Java objects, else prohibits .
    - default: `false`
    - **WARNING**: be very wary of parsing unsafe YAML. See [docs](/README.adoc#unsafe)
  - `:mark` - when `true` row and col position of YAML input is tracked and returned in alternate structure.
    - default: `false`
    - See [docs](/README.adoc#mark)

  Note: clj-yaml will only recognize the first of `:unsafe`, `:mark` or `:unknown-tag-fn`"
  [^String yaml-string & {:keys [keywords load-all unknown-tag-fn
                                 max-aliases-for-collections allow-recursive-keys allow-duplicate-keys
                                 unsafe mark] :or {keywords true}}]
  (let [yaml (make-yaml :unsafe unsafe
                        :mark mark
                        :unknown-tag-fn unknown-tag-fn
                        :max-aliases-for-collections max-aliases-for-collections
                        :allow-recursive-keys allow-recursive-keys
                        :allow-duplicate-keys allow-duplicate-keys)]
    (load-stream yaml (StringReader. yaml-string) load-all keywords unknown-tag-fn)))

(defn generate-stream
  ;; From https://github.com/metosin/muuntaja/pull/94/files
  "Dump the content of Clojure `data` as YAML into `writer`.

  See [[generate-string]] for `& opts`"
  [writer data & opts]
  (.dump ^Yaml (apply make-yaml opts) (encode data) writer))

(defn parse-stream
  "Returns yaml from `reader` as Clojure structures.

  See [[parse-string]] for `& opts`"
  [^java.io.Reader reader & {:keys [keywords load-all unknown-tag-fn] :or {keywords true} :as opts}]
  (load-stream (apply make-yaml (into [] cat opts))
               reader
               load-all keywords unknown-tag-fn))
