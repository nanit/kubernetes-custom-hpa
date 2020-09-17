(ns custom-hpa.test-helper.metric.core
  (:require [clojure.test :refer :all]
            [custom-hpa.metric.core :as metric]
            [custom-hpa.helpers.env :refer [double-env]]))

(def ^:dynamic metric-samples nil)

(defn- pop-sample []
  (let [sample (first @metric-samples)]
    (swap! metric-samples rest)
    sample))

(defn metric-fixture [f]
  (binding [metric-samples (atom [])]
    (with-redefs [metric/fetch pop-sample]
      (f))))

(def scale-up-min-sample (* (double-env "TARGET_VALUE") (+ 1.0 (double-env "SCALE_UP_MIN_FACTOR"))))
(def scale-up-max-sample (* (double-env "TARGET_VALUE") (+ 1.0 (double-env "SCALE_UP_MAX_FACTOR"))))
(def scale-down-min-sample (* (double-env "TARGET_VALUE") (- 1.0 (double-env "SCALE_DOWN_MIN_FACTOR"))))
(def scale-down-max-sample (* (double-env "TARGET_VALUE") (- 1.0 (double-env "SCALE_DOWN_MAX_FACTOR"))))

(defn sample [min-sample max-sample] (+ min-sample (rand (- max-sample min-sample))))

(defn- generate-samples
  [min-sample max-sample n]
  (reduce (fn [acc _] (conj acc (sample min-sample max-sample))) #{} (range n)))

(def scale-up-samples (partial generate-samples scale-up-min-sample scale-up-max-sample))
(def scale-down-samples (partial generate-samples scale-down-min-sample scale-down-max-sample))

(defn seed-samples [samples] (reset! metric-samples samples))

(defn seed-scale-up-samples [n] (seed-samples (scale-up-samples n)))
(defn seed-scale-down-samples [n] (seed-samples (scale-down-samples n)))