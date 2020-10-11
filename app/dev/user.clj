(ns user
  (:require [clojure.repl :refer :all]
            [clojure.tools.namespace.repl :refer [refresh refresh-all disable-reload!]]
            [clojure.test :refer [run-all-tests]]))

(defn run-tests []
  (refresh-all) (run-all-tests #"custom-hpa.*-test"))