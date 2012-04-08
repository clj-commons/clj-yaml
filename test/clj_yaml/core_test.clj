(ns clj-yaml.core-test
  (:use clojure.test)
  (:use clj-yaml.core))

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

(def inline-list-yaml "
--- # Shopping list
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

(def set-yaml "
--- !!set
? Mark McGwire
? Sammy Sosa
? Ken Griff")

(deftest parse-hash
  (let [parsed (parse-string "foo: bar")]
    (is (= "bar" (parsed :foo)))))

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

(deftest keywordized
  (binding [*keywordize* false]
    (is  (= "items" (-> hashes-lists-yaml parse-string ffirst))))
  (is  (= "items" (-> hashes-lists-yaml (parse-string false) ffirst))))

(deftest dump-opts
  (let [data [{:name "jon" :age 33} {:name "boo" :age 44}]]
    (is (= "- age: 33\n  name: jon\n- age: 44\n  name: boo\n"
           (generate-string data :dumper-options {:flow-style :block})))
    (is (= "[{age: 33, name: jon}, {age: 44, name: boo}]\n"
           (generate-string data :dumper-options {:flow-style :flow})))))