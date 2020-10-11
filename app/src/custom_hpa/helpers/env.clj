(ns custom-hpa.helpers.env
  (:require [clojure.string :as str]))

(defn- env [name] (str/trim (System/getenv name)))

(defn int-env [name] (Integer/parseInt (env name)))

(defn double-env [name] (Double/parseDouble (env name)))
