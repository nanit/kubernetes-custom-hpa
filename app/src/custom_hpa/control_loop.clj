(ns custom-hpa.control-loop
  (:require [taoensso.timbre :as logger]
            [clojure.core.async :refer [go-loop <! timeout]]
            [iapetos.core :as prometheus]
            [custom-hpa.helpers.env :refer [int-env]]
            [custom-hpa.control-loop.period :as period]
            [custom-hpa.monitor.prometheus :refer [registry]]
            [custom-hpa.clients.kube :as kube]))

(def ^:private period-ms (delay (* 1000 (int-env "CONTROL_LOOP_PERIOD"))))

(def ^:private client-ttl-ms
  ;; Re-init the kube-client well before the bound ServiceAccount token expires
  ;; (default token lifetime is 1h in K8s 1.21+). client-java 8.0.2 caches the
  ;; token at build time and never refreshes it, so the cached client gets 401
  ;; once kubelet rotates the token. 30 min is a safe margin.
  (* 30 60 1000))

(defn- needs-refresh? [last-init-ms]
  (> (- (System/currentTimeMillis) last-init-ms) client-ttl-ms))

(defn start [deployment deployment-namespace metric-provider]
  (logger/info "Starting control loop every" @period-ms "milliseconds, deployment =" deployment ", namespace = " deployment-namespace)
  (let [kube-client (atom (kube/init))
        last-init (atom (System/currentTimeMillis))]
    (go-loop []
      (try
        (when (needs-refresh? @last-init)
          (logger/info "Re-initializing kube-client to refresh ServiceAccount token")
          (reset! kube-client (kube/init))
          (reset! last-init (System/currentTimeMillis)))
        (prometheus/inc (registry :custom-hpa/up))
        (period/run metric-provider @kube-client deployment deployment-namespace)
        (catch Exception e
          (logger/error e "Exception was thrown during control loop period")
          ;; Force a re-init on the next iteration; covers transient API errors
          ;; AND the cached-expired-token case (which would otherwise loop forever).
          (reset! last-init 0))
        (finally
          (<! (timeout @period-ms))))
      (recur))))