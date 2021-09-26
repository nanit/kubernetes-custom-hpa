(ns custom-hpa.test-helper.kube
  (:require [clojure.test :refer :all]
            [custom-hpa.clients.kube :as kube])
  (:import [io.kubernetes.client.util ClientBuilder]
           [io.kubernetes.client.openapi.apis AppsV1Api]))

(def ^:dynamic deployment-default-spec {:name    (System/getenv "DEPLOYMENT")
                                        :ns      (System/getenv "NAMESPACE")
                                        :current 10
                                        :desired 10})

(def ^:dynamic deployment-fix nil)

(def dummy-client (AppsV1Api. (.build (ClientBuilder/standard))))

(defn- deployment? [deployment deployment-namespace]
  (and (= (:name @deployment-fix) deployment)
       (= (:ns @deployment-fix) deployment-namespace)))

(defn kube-fixture [f]
  (binding [deployment-fix (atom deployment-default-spec)]
    (with-redefs [kube/pods-count (fn [kube-client deployment deployment-namespace]
                                            (when (and (= dummy-client kube-client) (deployment? deployment deployment-namespace))
                                              (:current @deployment-fix)))
                  kube/scale-deployment (fn [kube-client deployment deployment-namespace desired-pods-count _dry-run?]
                                          (when (and (= dummy-client kube-client) (deployment? deployment deployment-namespace))
                                            (swap! deployment-fix assoc :current desired-pods-count :desired desired-pods-count)))]
      (f))))

(defmacro with-current-pods [current-pods & body]
  `(do (swap! deployment-fix assoc :current ~current-pods)
       ~@body))