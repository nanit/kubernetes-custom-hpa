(ns custom-hpa.test-helper.control-loop.status
  (:require [clojure.test :refer :all]
            [custom-hpa.control-loop.status :as status]))

(defn status-fixture [f]
  (status/clean)
  (f))