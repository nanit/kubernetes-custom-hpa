(ns custom-hpa.clients.kube
  (:require [taoensso.timbre :as logger]
            [clojure.data.json :as json])
  (:import [io.kubernetes.client.util ClientBuilder]
           [io.kubernetes.client.openapi Configuration]
           [io.kubernetes.client.openapi.apis AppsV1Api]
           [io.kubernetes.client.custom V1Patch]))

(def ^:private api (atom nil))
(def ^:private deployment (atom nil))
(def ^:private deployment-namespace (atom nil))
(def ^:private dry-run (atom nil))

(defn- generate-patch
  "Generates a V1Patch object to use for PATCH the deployment"
  [pods-count]
  (V1Patch. (json/write-str [{:op "replace" :path "/spec/replicas" :value pods-count}])))

(defn init
  "Initializes a kubernetes client for apps/v1 API group"
  [dep namespace dry-run?]
  (logger/debug "Initializing k8s client")
  (reset! deployment dep)
  (reset! deployment-namespace namespace)
  (reset! dry-run dry-run?)
  (let [client (.build (ClientBuilder/cluster))]
    (Configuration/setDefaultApiClient client)
    (reset! api (AppsV1Api.))))

(defn current-pods-count
  "Returns current deployment's number of pods"
  []
  (let [deployment (.readNamespacedDeployment @api @deployment @deployment-namespace nil nil nil)
        deployment-status (.getStatus deployment)]
    (.getReplicas deployment-status)))

(defn scale-deployment
  "Updates the deployment's desired number of pods with `pods-count`"
  [pods-count]
  (let [patch (generate-patch pods-count)]
    (logger/info "Going to scale deployment to" pods-count "pods")
    (.patchNamespacedDeployment @api @deployment @deployment-namespace patch nil @dry-run nil nil)))
