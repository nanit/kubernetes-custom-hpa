(ns custom-hpa.test-helper.kube
  (:require [clojure.test :refer :all]
            [custom-hpa.clients.kube :as kube]))

(def ^:dynamic deployment-default-spec {:current 10 :desired 10})
(def ^:dynamic deployment-fix nil)

(defn kube-fixture [f]
  (binding [deployment-fix (atom deployment-default-spec)]
    (with-redefs [kube/current-pods-count (fn [] (:current @deployment-fix))
                  kube/scale-deployment (fn [pods-count] (swap! deployment-fix assoc :current pods-count :desired pods-count))]
      (f))))

(defmacro with-current-pods [current-pods & body]
  `(do (swap! deployment-fix assoc :current ~current-pods)
       ~@body))