(ns custom-hpa.control-loop.common)

(def scale-up :up)
(def scale-down :down)

(defn scale-up? [factor] (> factor 1.0))
(def scale-down? (complement scale-up?))

(defn scale-type [factor]
  (if (scale-up? factor)
    scale-up
    scale-down))