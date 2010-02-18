(ns clj-yaml-test
  (:require clj-yaml)
  (:use (clj-unit core)))

(def nested-hash-yaml "root:\n  childa: a\n  childb: \n    grandchild: \n      greatgrandchild: bar\n")
(def list-yaml "--- # Favorite Movies\n- Casablanca\n- North by Northwest\n- The Man Who Wasn't There")
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

(def inline-hash-yaml "{name: John Smith, age: 33}")

(def list-of-hashes-yaml "
- {name: John Smith, age: 33}
- name: Mary Smith
  age: 27
")

(def hashes-of-lists "
men: [John Smith, Bill Jones]
women:
  - Mary Smith
  - Susan Williams
")

(deftest "parse hash"
  (let [parsed (clj-yaml/parse-string "foo: bar")]
    (assert= "bar" (parsed "foo"))))

(deftest "parse nested hash"
  (let [parsed (clj-yaml/parse-string nested-hash-yaml)]
    (assert= "a"     ((parsed "root") "childa"))
    (assert= "bar" ((((parsed "root") "childb") "grandchild") "greatgrandchild"))))

(deftest "parse list"
  (let [parsed (clj-yaml/parse-string list-yaml)]
    (assert= "Casablanca"               (first parsed))
    (assert= "North by Northwest"       (nth parsed 1))
    (assert= "The Man Who Wasn't There" (nth parsed 2))))

(deftest "parse nested hash and list"
  (let [parsed (clj-yaml/parse-string hashes-lists-yaml)]
    (assert= "A4786"  ((first (parsed "items")) "part_no"))
    (assert= "Dorthy" (first ((nth (parsed "items") 1) "owners")))))

(deftest "parse inline list"
  (let [parsed (clj-yaml/parse-string inline-list-yaml)]
    (assert= "milk"        (first parsed))
    (assert= "pumpkin pie" (nth parsed 1))
    (assert= "eggs"        (nth parsed 2))
    (assert= "juice"       (last parsed ))))

(deftest "parse inline hash"
  (let [parsed (clj-yaml/parse-string inline-hash-yaml)]
    (assert= "John Smith" (parsed "name"))
    (assert= 33 (parsed "age"))))

(deftest "parse list of hashes"
  (let [parsed (clj-yaml/parse-string list-of-hashes-yaml)]
    (assert= "John Smith" ((first parsed) "name"))
    (assert= 33           ((first parsed) "age"))
    (assert= "Mary Smith" ((nth parsed 1) "name"))
    (assert= 27           ((nth parsed 1) "age"))))

(deftest "hashes of lists"
  (let [parsed (clj-yaml/parse-string hashes-of-lists)]
    (assert= "John Smith"     (first (parsed "men")))
    (assert= "Bill Jones"     (last (parsed "men")))
    (assert= "Mary Smith"     (first (parsed "women")))
    (assert= "Susan Williams" (last (parsed "women")))))

