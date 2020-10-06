(ns custom-hpa.control-loop.executor
  (:require [taoensso.timbre :as logger]
            [iapetos.core :as prometheus]
            [custom-hpa.helpers.env :refer [int-env]]
            [custom-hpa.clients.kube :as kube]
            [custom-hpa.monitor.prometheus :refer [registry]]
            [custom-hpa.control-loop.common :refer [scale-up? scale-down? scale-type]]
            [custom-hpa.control-loop.status :as status]))

(def ^:private max-replicas (delay (int-env "MAX_REPLICAS")))
(def ^:private min-replicas (delay (int-env "MIN_REPLICAS")))
(def ^:private dry-run (delay (System/getenv "DRY_RUN")))

(defn- calculate-desired-pods-count
  [current-pods-count factor]
  (let [desired-pods-count (* current-pods-count factor)
        normalized-pods-count (max (min desired-pods-count @max-replicas) @min-replicas)
        rounded-pods-count (int (Math/ceil normalized-pods-count))]
    (logger/debug "Calculated new desired number of pods:" rounded-pods-count)
    (prometheus/set (registry :custom-hpa/status-desired-replicas) rounded-pods-count)
    rounded-pods-count))

(defn- scale* [kube-client deployment deployment-namespace factor current-pods-count]
  (let [desired-pods-count (calculate-desired-pods-count current-pods-count factor)]
    (kube/scale-deployment kube-client deployment deployment-namespace desired-pods-count @dry-run)
    (logger/info "Scaled deployment to" desired-pods-count "pods")
    (status/scaled (scale-type factor))))

(defn scale
  "Calculates desired number of pods and updates the deployment's spec replicas.
  The desired number of pods is calculated by multiplying `factor` and the current replicas from the deployment's status."
  [kube-client deployment deployment-namespace factor]
  (let [current-pods-count (kube/current-pods-count kube-client deployment deployment-namespace)]
    (logger/debug "Current pods count is" current-pods-count)
    (prometheus/set (registry :custom-hpa/status-current-replicas) current-pods-count)
    (scale* kube-client deployment deployment-namespace factor current-pods-count)))