(ns custom-hpa.control-loop.reconciler
  (:require [clj-time.core :as time]
            [custom-hpa.helpers.env :refer [int-env double-env]]
            [custom-hpa.control-loop.common :refer [scale-up scale-down scale-type]]
            [custom-hpa.control-loop.status :as status]
            [taoensso.timbre :as logger]))

(def ^:private scale-type->rules {scale-up   {:cooldown        (delay (int-env "SCALE_UP_COOLDOWN"))
                                              :min-factor      (delay (+ 1.0 (double-env "SCALE_UP_MIN_FACTOR")))
                                              :min-factor-pred <=}
                                  scale-down {:cooldown        (delay (int-env "SCALE_DOWN_COOLDOWN"))
                                              :min-factor      (delay (- 1.0 (double-env "SCALE_DOWN_MIN_FACTOR")))
                                              :min-factor-pred >=}})

(defn- cooldown? [scale-type]
  (when-let [last-scale-time (status/last-scale-time scale-type)]
    (let [cooldown-extension (:cooldown (scale-type->rules scale-type))
          last-scale-time-extended (time/plus last-scale-time (time/seconds @cooldown-extension))
          before? (time/before? (time/now) last-scale-time-extended)]
      (when before?
        (logger/info "Can't scale" (name scale-type) "because custom HPA is on cooldown")
        (status/event scale-type status/cooldown))
      before?)))

(defn- lt-min-factor? [factor scale-type]
  (let [{:keys [min-factor min-factor-pred]} (scale-type->rules scale-type)
        lt? (min-factor-pred factor @min-factor)]
    (when lt?
      (logger/info "Can't scale" (name scale-type) "because scale factor" factor "is below minimum" @min-factor)
      (status/event scale-type status/below-min-factor))
    lt?))

(defn scale-allowed?
  "Validates that custom HPA is not in cooldown and the desired scale is above the minimum"
  [factor]
  (let [scale-type (scale-type factor)]
    (and (not (cooldown? scale-type))
         (not (lt-min-factor? factor scale-type)))))
