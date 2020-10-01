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

(defn- calculate-desired-pods-count
  [pods-count factor]
  (let [pods-count (* pods-count factor)
        normalized-pods-count (cond
                                (and (> pods-count pods-count) (> pods-count @max-replicas)) @max-replicas
                                (and (< pods-count pods-count) (< pods-count @min-replicas)) @min-replicas
                                :default pods-count)
        rounded-pods-count (int (Math/ceil normalized-pods-count))]
    (logger/debug "Calculated new desired number of pods:" rounded-pods-count)
    (prometheus/set (registry :custom-hpa/status-desired-replicas) rounded-pods-count)
    rounded-pods-count))

(defn- should-scale? [factor current-pods-count]
  (or
    (and (scale-up? factor) (not= @max-replicas current-pods-count))
    (and (scale-down? factor) (not= @min-replicas current-pods-count))))

(defn- scale* [factor current-pods-count]
  (let [desired-pods-count (calculate-desired-pods-count current-pods-count factor)]
    (kube/scale-deployment desired-pods-count)
    (logger/info "Scaled deployment to" desired-pods-count "pods")
    (status/notify (scale-type factor) status/scaled)))

(defn scale
  "Calculates desired number of pods and updates the deployment's spec replicas.
  The desired number of pods is calculated by multiplying `factor` and the current replicas from the deployment's status."
  [factor]
  (let [current-pods-count (kube/current-pods-count)]
    (logger/debug "Current pods count is" current-pods-count)
    (prometheus/set (registry :custom-hpa/status-current-replicas) current-pods-count)
    (if (should-scale? factor current-pods-count)
      (scale* factor current-pods-count)
      (do (logger/error "Can't scale deployment. Current pods =" current-pods-count ", Max replicas =" @max-replicas ", Min replicas =" @min-replicas)
          (status/notify (scale-type factor) status/limited)))))