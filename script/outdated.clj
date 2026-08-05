(ns outdated
  (:require [helper.shell :as shell]))

(defn task [_opts]
  (shell/clojure {:continue true}
                 "-M:outdated --directory=.:nvd_check_helper_project"))
