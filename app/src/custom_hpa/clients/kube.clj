(ns custom-hpa.clients.kube
  (:require [taoensso.timbre :as logger]
            [clojure.data.json :as json])
  (:import [io.kubernetes.client.util ClientBuilder]
           [io.kubernetes.client.openapi.apis AppsV1Api]
           [io.kubernetes.client.custom V1Patch]))

(def ^:private api (atom nil))

(defn- generate-patch
  "Generates a V1Patch object to use for PATCH the deployment"
  [pods-count]
  (V1Patch. (json/write-str [{:op "replace" :path "/spec/replicas" :value pods-count}])))

(defn init
  "Initializes a kubernetes client for apps/v1 API group"
  []
  (logger/debug "Initializing k8s client")
  (let [client (.build (doto (ClientBuilder/cluster)
                         (.setVerifyingSsl false)))]
    (reset! api (AppsV1Api. client))))

(defn current-pods-count
  "Returns current deployment's number of pods"
  [deployment deployment-namespace]
  (let [deployment (.readNamespacedDeployment @api deployment deployment-namespace nil nil nil)
        deployment-status (.getStatus deployment)]
    (.getReplicas deployment-status)))

(defn scale-deployment
  "Updates the deployment's desired number of pods with `desired-pods-count`."
  ([deployment deployment-namespace desired-pods-count]
   (scale-deployment deployment deployment-namespace desired-pods-count false))
  ([deployment deployment-namespace desired-pods-count dry-run?]
  (let [patch (generate-patch desired-pods-count)
        dry-run (when dry-run? "All")]
    (logger/info "Going to scale deployment to" desired-pods-count "pods")
    (.patchNamespacedDeployment @api deployment deployment-namespace patch nil dry-run nil nil))))
