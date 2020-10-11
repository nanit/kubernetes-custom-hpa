(ns custom-hpa.metric.core
  (:require [taoensso.timbre :as logger]
            [iapetos.core :as prometheus]
            [custom-hpa.monitor.prometheus :refer [registry]]
            [custom-hpa.metric.protocol.prometheus :refer [->Prometheus]]
            [custom-hpa.metric.protocol.provider :as provider]))

(defn default-provider []
  (let [prometheus-url (System/getenv "PROMETHEUS_URL")
        prometheus-port (System/getenv "PROMETHEUS_PORT")
        prometheus-host (format "%s:%s/api/v1/query" prometheus-url prometheus-port)
        prometheus-query (System/getenv "PROMETHEUS_QUERY")]
    (->Prometheus prometheus-host prometheus-query)))

(defn- report [sample]
  (if sample
    (do
      (prometheus/inc (registry :custom-hpa/metric-requests {:status "success"}))
      (prometheus/set (registry :custom-hpa/metric-samples) sample))
    (prometheus/inc (registry :custom-hpa/metric-requests {:status "failure"}))))

(defn fetch [provider]
  (let [sample (provider/fetch provider)]
    (logger/debug "Fetched metric sample:" sample)
    (report sample)
    sample))