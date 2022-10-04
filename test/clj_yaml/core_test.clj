(ns clj-yaml.core-test
  (:require
   [clj-yaml.core :as yaml :refer [generate-stream generate-string
                                   parse-stream parse-string unmark]]
   [clj-yaml.test-report]
   [clojure.java.io :as io]
   [clojure.string :as string]
   [clojure.test :refer (deftest testing is)]
   [flatland.ordered.map :refer [ordered-map]])
  (:import
   (java.io ByteArrayInputStream ByteArrayOutputStream OutputStreamWriter)
   java.nio.charset.StandardCharsets
   [java.util Date]
   [org.yaml.snakeyaml.composer ComposerException]
   (org.yaml.snakeyaml.constructor ConstructorException DuplicateKeyException)
   (org.yaml.snakeyaml.constructor DuplicateKeyException)
   (org.yaml.snakeyaml.error YAMLException)))

(def nested-hash-yaml
  "root:\n  childa: a\n  childb: \n    grandchild: \n      greatgrandchild: bar\n")

(def list-yaml
  "--- # Favorite Movies\n- Casablanca\n- North by Northwest\n- The Man Who Wasn't There")

(def hashes-lists-yaml "
items:
  - part_no:   A4786
    descrip:   Water Bucket (Filled)
    price:     1.47
    quantity:  4

  - part_no:   E1628
    descrip:   High Heeled \"Ruby\" Slippers
    price:     100.27
    quantity:  1
    owners:
      - Dorthy
      - Wicked Witch of the East
")

(def inline-list-yaml
  "--- # Shopping list
[milk, pumpkin pie, eggs, juice]
")

(def inline-hash-yaml
  "{name: John Smith, age: 33}")

(def list-of-hashes-yaml "
- {name: John Smith, age: 33}
- name: Mary Smith
  age: 27
")

(def hashes-of-lists-yaml "
men: [John Smith, Bill Jones]
women:
  - Mary Smith
  - Susan Williams
")

(def typed-data-yaml "
the-bin: !!binary 0101")

(def io-file-typed-data-yaml "
!!java.io.File")

(def set-yaml "
--- !!set
? Mark McGwire
? Sammy Sosa
? Ken Griff")

(deftest parse-hash
  (let [parsed (parse-string "foo: bar")]
    (is (= "bar" (parsed :foo)))))

(deftest parse-hash-with-numeric-key
  (let [parsed (parse-string "123: 456")]
    (is (= 456 (parsed 123)))))

(deftest parse-hash-with-complex-key
  (let [parsed (parse-string "[1, 2]: 3")]
    (is (= 3 (parsed [1, 2])))))

(deftest parse-nested-hash
  (let [parsed (parse-string nested-hash-yaml)]
    (is (= "a"   ((parsed :root) :childa)))
    (is (= "bar" ((((parsed :root) :childb) :grandchild) :greatgrandchild)))))

(deftest parse-list
  (let [parsed (parse-string list-yaml)]
    (is (= "Casablanca"               (first parsed)))
    (is (= "North by Northwest"       (nth parsed 1)))
    (is (= "The Man Who Wasn't There" (nth parsed 2)))))

(deftest parse-nested-hash-and-list
  (let [parsed (parse-string hashes-lists-yaml)]
    (is (= "A4786"  ((first (parsed :items)) :part_no)))
    (is (= "Dorthy" (first ((nth (parsed :items) 1) :owners))))))

(deftest parse-inline-list
  (let [parsed (parse-string inline-list-yaml)]
    (is (= "milk"        (first parsed)))
    (is (= "pumpkin pie" (nth   parsed 1)))
    (is (= "eggs"        (nth   parsed 2)))
    (is (= "juice"       (last  parsed)))))

(deftest parse-inline-hash
  (let [parsed (parse-string inline-hash-yaml)]
    (is (= "John Smith" (parsed :name)))
    (is (= 33           (parsed :age)))))

(deftest parse-list-of-hashes
  (let [parsed (parse-string list-of-hashes-yaml)]
    (is (= "John Smith" ((first parsed) :name)))
    (is (= 33           ((first parsed) :age)))
    (is (= "Mary Smith" ((nth parsed 1) :name)))
    (is (= 27           ((nth parsed 1) :age)))))

(deftest hashes-of-lists
  (let [parsed (parse-string hashes-of-lists-yaml)]
    (is (= "John Smith"     (first (parsed :men))))
    (is (= "Bill Jones"     (last  (parsed :men))))
    (is (= "Mary Smith"     (first (parsed :women))))
    (is (= "Susan Williams" (last  (parsed :women))))))

(deftest h-set
  (is (= #{"Mark McGwire" "Ken Griff" "Sammy Sosa"}
         (parse-string set-yaml))))

(deftest typed-data
  (let [parsed (parse-string typed-data-yaml)]
    (is (= (Class/forName "[B") (type (:the-bin parsed))))))

(deftest disallow-arbitrary-typed-data
  (is (thrown? org.yaml.snakeyaml.error.YAMLException
               (parse-string io-file-typed-data-yaml))))

(deftest keywordized
  (is (= "items"
         (-> hashes-lists-yaml
             (parse-string :keywords false)
             ffirst))))

(deftest not-keywordized-in-lists
  (is (every? string?
              (-> "[{b: c, c: d}]"
                  (parse-string :keywords false)
                  first
                  keys))))

(deftest unconvertable-key-not-converted-to-keyword
  ;; we are not sophisticated here, but do handle some cases
  (is (= {42 1 :b 2}
         (parse-string "{42: 1, b: 2}" :keywords true))))

(deftest key-fn-option
  (is (= {:a 1}
         (parse-string "{a: 1}" :key-fn #(-> % :key keyword)))
      "can operate like :keywords true")
  (is (= {"A" 1}
         (parse-string "{a: 1}" :key-fn #(-> % :key string/upper-case)))
      "overrides default :keywords true")
  (is (= {"A" 1}
         (parse-string "{a: 1}"
                       :keywords false
                       :key-fn #(-> % :key string/upper-case)))
      "overrides :keywords false")
  (is (= {"A" 1}
         (parse-string "{a: 1}"
                       :keywords true
                       :key-fn #(-> % :key string/upper-case)))
      "overrides :keywords true")
  (is (= {"BA" 1}
         (parse-string "{!blam ab: 1}"
                       :key-fn #(-> % :key string/upper-case)
                       :unknown-tag-fn #(-> % :value string/reverse)))
      "can be combined with :unknown-tag-fn"))

(deftest marking-source-position-works
  (let [parsed (parse-string inline-list-yaml :mark true)]
    ;; The list starts at the beginning of line 1.
    (is (= 1 (-> parsed :start :line)))
    (is (= 0 (-> parsed :start :column)))
    ;; The first item starts at the second character of line 1.
    (is (= 1 (-> parsed unmark first :start :line)))
    (is (= 1 (-> parsed unmark first :start :column)))
    ;; The first item ends at the fifth character of line 1.
    (is (= 1 (-> parsed unmark first :end :line)))
    (is (= 5 (-> parsed unmark first :end :column)))))

(deftest text-wrapping
  (let [data
        {:description
         "Big-picture diagram showing how our top-level systems and stakeholders interact"}]
    (testing "long lines of text should not be wrapped"
      ;; clj-yaml 0.5.6 used SnakeYAML 1.13 which by default did *not* split long lines.
      ;; clj-yaml 0.6.0 upgraded to SnakeYAML 1.23 which by default *did* split long lines.
      ;; This test ensures that generate-string uses the older behavior by default, for the sake
      ;; of stability, i.e. backwards compatibility.
      (is
       (= "{description: Big-picture diagram showing how our top-level systems and stakeholders interact}\n"
          (generate-string data))))))

(deftest dump-opts
  (let [data [{:age 33 :name "jon"} {:age 44 :name "boo"}]]
    (is (= "- age: 33\n  name: jon\n- age: 44\n  name: boo\n"
           (generate-string data :dumper-options {:flow-style :block})))
    (is (= "[{age: 33, name: jon}, {age: 44, name: boo}]\n"
           (generate-string data :dumper-options {:flow-style :flow})))))

(deftest parse-time
  (testing "clj-time parses timestamps with more than millisecond precision correctly."
    (let [timestamp "2001-11-23 15:02:31.123456 -04:00"
          expected 1006542151123]
      (is (= (.getTime ^Date (parse-string timestamp)) expected)))))

(deftest maps-are-ordered
  (let [parsed (parse-string hashes-lists-yaml)
        [first second] (:items parsed)]
    (is (= (keys first) '(:part_no :descrip :price :quantity)))
    (is (= (keys second) '(:part_no :descrip :price :quantity :owners)))))


(deftest nulls-are-fine
  (testing "nil does not blow up"
    (let [res (parse-string "- f:")]
      (is (= [{:f nil}] res))
      (is (str res)))))

(deftest emoji-can-be-parsed
  (let [yaml "{emoji: 💣}"]
    (is (= yaml (-> yaml
                    (generate-string)
                    (parse-string)
                    (string/trim)))))

  (testing "emoji in comments are OK too"
    (let [yaml "# 💣 emoji in a comment\n42"]
      (is (= 42 (parse-string yaml))))))

(def too-many-aliases
  (->> (range 51)
       (map #(str "b" % ": *a"))
       (cons "a: &a [\"a\",\"a\"]")
       (string/join "\n")))

(deftest max-aliases-for-collections-works
  (is (thrown-with-msg? YAMLException #"Number of aliases" (parse-string too-many-aliases))
      "throws when default of 50 is exceeded")
  (is (parse-string too-many-aliases :max-aliases-for-collections 51)
      "passes when we bump max to 51"))

(def recursive-yaml "
---
&A
- *A: *A
")

(deftest allow-recursive-works
  (is (thrown-with-msg? YAMLException #"Recursive" (parse-string recursive-yaml)))
  (is (parse-string recursive-yaml :allow-recursive-keys true)))

(def duplicate-keys-yaml "
a: 1
a: 1
")

(deftest duplicate-keys-works
  (is (parse-string duplicate-keys-yaml))
  (is (thrown-with-msg? DuplicateKeyException #"found duplicate key" (parse-string duplicate-keys-yaml :allow-duplicate-keys false))))

(def namespaced-keys-yaml "
foo/bar: 42
")

(deftest namespaced-keys-works
  (testing "namespaced keys in yaml can round trip through parse and generate"
    (is (= {:foo/bar 42} (-> namespaced-keys-yaml
                             parse-string
                             generate-string
                             parse-string)))))

(defn to-bytes
  "Converts a string to a byte array."
  [data]
  (.getBytes ^String data StandardCharsets/UTF_8))

(defn roundtrip
  "Testing roundtrip of string and stream parser, and checking their equivalence."
  [data-as-string]
  (let [data (parse-string data-as-string)
        data-stream (parse-stream (io/reader (ByteArrayInputStream. (to-bytes data-as-string))))
        output-stream (ByteArrayOutputStream.)
        writer (OutputStreamWriter. output-stream)
        _ (generate-stream writer data)
        reader (ByteArrayInputStream. (.toByteArray output-stream))]
    (= data ;; string -> edn
       (parse-string (generate-string data)) ;; edn -> string -> edn
       (parse-stream (io/reader reader)) ;; edn -> stream -> edn
       ;; stream -> edn
       data-stream)))

(deftest roundtrip-test
  (testing "Roundtrip test"
    (is (roundtrip duplicate-keys-yaml))
    (is (roundtrip hashes-of-lists-yaml))
    (is (roundtrip inline-hash-yaml))
    (is (roundtrip inline-list-yaml))
    (is (roundtrip list-of-hashes-yaml))
    (is (roundtrip list-yaml))
    (is (roundtrip nested-hash-yaml))))

(defn- ->stream [string]
  (io/reader (.getBytes ^String string)))

(def multi-doc-yaml "
---
foo: true
---
bar: false")

(def single-doc-yaml "
---
lol: yolo")

(deftest load-all-test
  (testing "Without load-all"
    (is (= (ordered-map {:lol "yolo"})
           (parse-string single-doc-yaml)))
    (is (= (ordered-map {:lol "yolo"})
           (parse-stream (->stream single-doc-yaml))))
    (is (thrown-with-msg? ComposerException #"expected a single document in the stream\n"
                          (parse-stream (->stream multi-doc-yaml))))
    (is (thrown-with-msg? ComposerException #"expected a single document in the stream\n"
                          (parse-string multi-doc-yaml))))

  (testing "With load-all=true on single docs"
    (is (= [(ordered-map {:lol "yolo"})]
           (parse-string single-doc-yaml :load-all true)))
    (is (= [(ordered-map {:lol "yolo"})]
           (parse-stream (->stream single-doc-yaml) :load-all true))))

  (testing "With load-all=true on multi docs"
    (is (= [(ordered-map {:foo true}) (ordered-map {:bar false})]
           (parse-string multi-doc-yaml :load-all true)))
    (is (= [(ordered-map {:foo true}) (ordered-map {:bar false})]
           (parse-stream (->stream multi-doc-yaml) :load-all true))))
  )

(def indented-yaml "todo:
  -  name: Fix issue
     responsible:
          name: Rita
")

(deftest indentation-test
  (testing "Can use indicator-indent and indent to achieve desired indentation"
    (is (not= indented-yaml (generate-string (parse-string indented-yaml)
                                             :dumper-options {:flow-style :block})))
    (is (= indented-yaml
           (generate-string (parse-string indented-yaml)
                            :dumper-options {:indent 5
                                             :indicator-indent 2
                                             :flow-style :block})))))

(def yaml-with-unknown-tags "---
scalar: !CustomScalar some-scalar
mapping: !CustomMapping
  x: foo
  y: bar
sequence: !CustomSequence
  - a
  - b
  - z
")

(deftest unknown-tags-test
  (testing "Throws with unknown tags and default constructor"
    (is (thrown-with-msg? ConstructorException
                          #"^could not determine a constructor for the tag !CustomScalar"
                          (parse-string yaml-with-unknown-tags))))
  (testing "Can process unknown tags with strip-unknown-tags? constructor"
    (is (= {:scalar "some-scalar"
            :mapping {:x "foo" :y "bar"}
            :sequence ["a" "b" "z"]}
           (parse-string yaml-with-unknown-tags :unknown-tag-fn :value))))
  (testing "Can process unknown tags with :unknown-tag-fn as identity"
    (is (= {:scalar {:tag "!CustomScalar" :value "some-scalar"}
            :mapping {:tag "!CustomMapping" :value {:x "foo" :y "bar"}}
            :sequence {:tag "!CustomSequence" :value ["a" "b" "z"]}}
           (parse-string yaml-with-unknown-tags :unknown-tag-fn identity)))
    (is (= {:base-12 12 :base-10 "10"}
           (parse-string "{base-12: !Base12 10, base-10: !Base10 10}"
                         :unknown-tag-fn (fn [{:keys [tag value]}]
                                           (if (= "!Base12" tag)
                                             (Integer/parseInt value 12) value)))))))

(deftest leading-zero-test
  (testing "leading zero is quoted"
    (is (= "['083']"
           (string/trim (generate-string ["083"]))))))

(def dangerous-yaml "!!javax.script.ScriptEngineManager [!!java.net.URLClassLoader [[!!java.net.URL [\"very-bad-badness-here\"]]]]")

(deftest unsafe-deny-test
  (is (thrown-with-msg? YAMLException #"(?m).*could not.*constructor.*ScriptEngineManager"
                        (parse-string dangerous-yaml)
                        "by default, SnakeYaml stops creation of classes - malicious example"))
  (is (thrown-with-msg? YAMLException #"(?m).*could not.*constructor.*java\.lang\.Long"
                        (parse-string "!!java.lang.Long 5")
                        "by default, SnakeYaml stops creation of classes - innocuous looking class example")))

(deftest unsafe-allow-test
  ;; be very wary of permitting unsafe class construction!
  (let [exm (try (parse-string dangerous-yaml :unsafe true)
                 (catch Throwable e
                   (Throwable->map e)))]
    (is (= (:cause exm) "no protocol: very-bad-badness-here")
        "SnakeYAML can be asked to create unsafe classes (it tried to create that nested URL) - malicious example"))
  (let [parsed (parse-string "!!java.lang.Long 5" :unsafe true)]
    (is (= 5 parsed) "SnakeYAML can be asked to create innocuous looking classes - value match")
    (is (= "class java.lang.Long" (str (class parsed))) "SnakeYAML can be asked to create innocuous looking classes - type match")))

(deftest low-level-decode-legacy-compat-test
  ;; Dear reader, we don't want to encourage you to use low level functions in any way,
  ;; this test is here to verify that we are compatible with existing code in the wild
  (let [to-decode (doto (java.util.LinkedHashMap.) (.put "a" 1))]
    (is (= (ordered-map {"a" 1})
           (yaml/decode to-decode false))
        "decode supports legacy [data keywords] signature - keywords false")
    (is (= (ordered-map {:a 1})
           (yaml/decode to-decode true))
        "decode supports legacy [data keywords] signature - keywords true")

    (is (= (ordered-map {"a" 1})
           (yaml/decode to-decode nil))
        "decode supports legacy [data keywords] signature - keywords nil")

    (is (= (ordered-map {"a" 1})
           (yaml/decode to-decode {}))
        "decode supports new [data opts] signature - keywords not specified")

    (is (= (ordered-map {:a 1})
           (yaml/decode to-decode {:keywords true}))
        "decode supports new [data opts] signature - keywords specified true")

    (is (= (ordered-map {"a" 1})
           (yaml/decode to-decode {:keywords false}))
        "decode supports new [data opts] signature - keywords specified true")))
