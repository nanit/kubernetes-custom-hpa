(ns custom-hpa.control-loop.status
  (:require [clj-time.core :as time]
            [custom-hpa.control-loop.common :refer [scale-up scale-down scale-up?]]
            [custom-hpa.monitor.prometheus :refer [registry]]
            [clojure.set :as set]
            [iapetos.core :as prometheus]))

(def cooldown "cooldown")
(def below-min-factor "below-min-factor")
(def above-max-factor "above-max-factor")
(def limited "limited")
(def scale "scaled")

(def ^:private last-scale-event-timestamp (atom {scale-up   nil
                                                 scale-down nil}))

(defn- event [scale-type status] {:scale (name scale-type) :status status})

(def ^:private all-events (atom (reduce
                                  (fn [acc status]
                                    (conj acc
                                          (event scale-up status)
                                          (event scale-down status)))
                                  #{}
                                  [cooldown below-min-factor above-max-factor limited scale])))

(def ^:private active-events (atom #{}))

(defn last-scale-time
  [scale-type]
  (@last-scale-event-timestamp scale-type))

(defn notify
  [scale-type status]
  (swap! active-events conj (event scale-type status)))

(defn scaled
  [scale-type]
  (notify scale-type scale)
  (swap! last-scale-event-timestamp assoc scale-type (time/now)))

(defn report []
  (let [non-active-events (set/difference @all-events @active-events)]
    (doseq [e @active-events] (prometheus/set (registry :custom-hpa/status e) 1))
    (doseq [e non-active-events] (prometheus/set (registry :custom-hpa/status e) 0))))

(defn init [] (reset! active-events #{}))

(defn clean [] (reset! last-scale-event-timestamp {scale-up nil scale-down nil}))

(defn notified? [scale-type status] (some #{(event scale-type status)} @active-events))