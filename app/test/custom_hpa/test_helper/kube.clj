(ns custom-hpa.test-helper.kube
  (:require [clojure.test :refer :all]
            [custom-hpa.clients.kube :as kube]))

(def ^:dynamic deployment-default-spec {:name    (System/getenv "DEPLOYMENT")
                                        :ns      (System/getenv "NAMESPACE")
                                        :current 10
                                        :desired 10})

(def ^:dynamic deployment-fix nil)

(defn- deployment? [deployment deployment-namespace]
  (and (= (:name @deployment-fix) deployment)
       (= (:ns @deployment-fix) deployment-namespace)))

(defn kube-fixture [f]
  (binding [deployment-fix (atom deployment-default-spec)]
    (with-redefs [kube/current-pods-count (fn [deployment deployment-namespace]
                                            (when (deployment? deployment deployment-namespace)
                                              (:current @deployment-fix)))
                  kube/scale-deployment (fn [deployment deployment-namespace desired-pods-count]
                                          (when (deployment? deployment deployment-namespace)
                                            (swap! deployment-fix assoc :current desired-pods-count :desired desired-pods-count)))]
      (f))))

(defmacro with-current-pods [current-pods & body]
  `(do (swap! deployment-fix assoc :current ~current-pods)
       ~@body))