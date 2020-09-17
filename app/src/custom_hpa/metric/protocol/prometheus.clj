(ns custom-hpa.metric.protocol.prometheus
  (:require [custom-hpa.metric.protocol.provider :refer [Provider]]
            [org.httpkit.client :as http]
            [clojure.data.json :as json]))

(def ^:private url (delay (System/getenv "PROMETHEUS_URL")))
(def ^:private port (delay (System/getenv "PROMETHEUS_PORT")))
(def ^:private query (delay (System/getenv "PROMETHEUS_QUERY")))

(def ^:private endpoint (delay (format "%s:%s/api/v1/query" url port)))
(def ^:private opts (delay {:timeout 1000 :query-params {:query @query}}))

(defn- parse-double [val] (Double/parseDouble val))

(defn- parse-response [body]
  (-> body
      :data
      :result
      first
      :value
      second
      parse-double))

(defn- response-ok?
  [status error body]
  (and (= 200 status)
       (nil? error)
       (= "success" (:status body))))

(defrecord Prometheus []
  Provider
  (fetch [this]
    (let [{:keys [status error body]} @(http/get @endpoint @opts)
          parsed-body (when body (json/read-str :key-fn keyword))]
      (when (response-ok? status error parsed-body)
        (parse-response body)))))