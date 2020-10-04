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

(def ^:private all-events (atom (reduce
                                  (fn [acc event]
                                    (conj acc
                                          {:scale scale-up :status event}
                                          {:scale scale-down :status event}))
                                  #{}
                                  [cooldown below-min-factor above-max-factor limited])))

(def ^:private active-events (atom #{}))

(defn- event [scale-type status] {:scale (name scale-type) :status status})

(defn last-scale-time
  [scale-type]
  (@last-scale-event-timestamp scale-type))

(defn scaled
  [scale-type]
  (swap! last-scale-event-timestamp assoc scale-type (time/now)))

(defn notify
  [scale-type status]
  (swap! active-events conj (event scale-type status)))

(defn report []
  (let [non-active-events (set/difference @all-events (map :status @active-events))]
    (doseq [e @active-events] (prometheus/set (registry :custom-hpa/status e) 1))
    (doseq [e non-active-events] (prometheus/set (registry :custom-hpa/status e) 0))))

(defn init [] (reset! active-events #{}))

(defn clean [] (reset! last-scale-event-timestamp {scale-up nil scale-down nil}))

(defn notified? [scale-type status] (some #{(event scale-type status)} @active-events))