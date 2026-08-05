(ns lint-eastwood
  (:require [compile-java]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(defn lint []
  (compile-java/task {})
  (status/line :head "eastwood: linting")
  (shell/command "clojure -M:test:eastwood"))

(defn task
  [_opts]
  (lint))
