(ns custom-hpa.metric.core
  (:require [taoensso.timbre :as logger]
            [iapetos.core :as prometheus]
            [custom-hpa.monitor.prometheus :refer [registry]]
            [custom-hpa.metric.protocol.prometheus :refer [->Prometheus]]
            [custom-hpa.metric.protocol.provider :as provider]))

(def ^:private provider-impl (->Prometheus))

(defn- report [sample]
  (if sample
    (do
      (prometheus/inc (registry :custom-hpa/metric-requests {:status "success"}))
      (prometheus/set (registry :custom-hpa/metric-requests) sample))
    (prometheus/inc (registry :custom-hpa/metric-requests {:status "failure"}))))

(defn fetch []
  (let [sample (provider/fetch provider-impl)]
    (logger/debug "Fetched metric sample:" sample)
    (report sample)
    sample))