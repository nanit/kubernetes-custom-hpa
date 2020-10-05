(ns custom-hpa.control-loop.period
  (:require [taoensso.timbre :as logger]
            [custom-hpa.metric.core :as metric]
            [custom-hpa.control-loop.factor :as factor]
            [custom-hpa.control-loop.reconciler :as reconciler]
            [custom-hpa.control-loop.executor :as executor]
            [custom-hpa.control-loop.status :as status]))

(defn run [deployment deployment-namespace metric-provider]
  (logger/debug "Control loop period started")
  (status/init)
  (when-let [metric-sample (metric/fetch metric-provider)]
    (let [target-factor (factor/calculate metric-sample)]
      (when (reconciler/scale-allowed? target-factor)
        (executor/scale deployment deployment-namespace target-factor))))
  (status/report)
  (logger/debug "Control loop period ended"))