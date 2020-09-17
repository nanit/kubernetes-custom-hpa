(ns custom-hpa.monitor.prometheus
  (:require [iapetos.core :as prometheus]
            [iapetos.export :as export]
            [custom-hpa.helpers.env :refer [int-env double-env]]))

(def ^:private deployment (delay (System/getenv "DEPLOYMENT")))
(def ^:private max-replicas (delay (int-env "MAX_REPLICAS")))
(def ^:private min-replicas (delay (int-env "MIN_REPLICAS")))
(def ^:private target-value (delay (double-env "TARGET_VALUE")))

(defonce registry*
  (-> (prometheus/collector-registry)
      (prometheus/register
        (prometheus/counter :custom-hpa/up {:description "a counter that is incremented every period"
                                            :labels      [:deployment]})
        (prometheus/gauge :custom-hpa/status {:description "the status of each period"
                                              :labels      [:deployment :status :scale]})
        (prometheus/gauge :custom-hpa/status-current-replicas {:description "the current number of pods fetched from the deployment's status"
                                                               :labels      [:deployment]})
        (prometheus/gauge :custom-hpa/status-desired-replicas {:description "the calculated desired number of pods"
                                                               :labels      [:deployment]})
        (prometheus/gauge :custom-hpa/scale-factor {:description "calculated scale factor"
                                                    :labels      [:deployment :status]})
        (prometheus/counter :custom-hpa/metric-requests {:description "counts number of success/failed metrics fetches"
                                                         :labels      [:deployment :status]})
        (prometheus/gauge :custom-hpa/metric-samples {:description "samples of fetched metric"
                                                      :labels      [:deployment]})
        (prometheus/gauge :custom-hpa/spec-min-replicas {:description "the custom HPA minimum pods"
                                                         :labels      [:deployment]})
        (prometheus/gauge :custom-hpa/spec-max-replicas {:description "the custom HPA maximum pods"
                                                         :labels      [:deployment]})
        (prometheus/gauge :custom-hpa/spec-target-value {:description "the custom HPA target value"
                                                         :labels      [:deployment]}))))

(defn registry
  ([metric]
   (registry metric {}))
  ([metric labels]
   (registry* metric (merge labels {:deployment @deployment}))))

(defn export []
  (export/text-format (-> registry*
                          (prometheus/set :custom-hpa/spec-min-replicas @min-replicas)
                          (prometheus/set :custom-hpa/spec-max-replicas @max-replicas)
                          (prometheus/set :custom-hpa/spec-target-value @target-value))))