(ns compile-java
  (:require [babashka.fs :as fs]
            [helper.shell :as shell]))

(defn task [_opts]
  (when (not (fs/exists? "target/classes"))
    (shell/clojure "-T:build compile-java")))
