(ns custom-hpa.control-loop.factor
  (:require [taoensso.timbre :as logger]
            [iapetos.core :as prometheus]
            [custom-hpa.helpers.env :refer [double-env]]
            [custom-hpa.monitor.prometheus :refer [registry]]
            [custom-hpa.control-loop.common :refer [scale-up? scale-down? scale-up scale-down]]
            [custom-hpa.control-loop.status :as status]))

(def ^:private target-value (delay (double-env "TARGET_VALUE")))
(def ^:private scale-up-max-factor (delay (+ 1.0 (double-env "SCALE_UP_MAX_FACTOR"))))
(def ^:private scale-down-max-factor (delay (- 1.0 (double-env "SCALE_DOWN_MAX_FACTOR"))))

(defn- normalize [factor]
  (prometheus/set (registry :custom-hpa/scale-factor) factor)
  (cond
    (and (scale-up? factor) (> factor @scale-up-max-factor))
    (do (logger/info "Scale up factor is too high, using max factor for scale up" @scale-up-max-factor)
        (status/notify scale-up status/above-max-factor)
        @scale-up-max-factor)

    (and (scale-down? factor) (< factor @scale-down-max-factor))
    (do (logger/info "Scale down factor is too low, using min factor for scale down" @scale-down-max-factor)
        (status/notify scale-up status/above-max-factor)
        @scale-down-max-factor)

    :default factor))

(defn calculate
  [metric-value]
  (let [factor (normalize (/ metric-value @target-value))]
    (logger/debug "calculated factor:" factor)
    factor))