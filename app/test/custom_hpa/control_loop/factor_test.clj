(ns custom-hpa.control-loop.factor-test
  (:require [clojure.test :refer :all]
            [custom-hpa.helpers.env :refer [double-env]]
            [custom-hpa.control-loop.factor :as factor]))

(deftest factor-bigger-than-scale-up-max-factor-test
  (testing "should return scale up max factor"
    (is (= (+ 1.0 (double-env "SCALE_UP_MAX_FACTOR")) (factor/calculate 150.0)))))

(deftest factor-smaller-than-scale-down-max-factor-test
  (testing "should return scale down max factor"
    (is (= (- 1.0 (double-env "SCALE_DOWN_MAX_FACTOR")) (factor/calculate 94.0)))))

(deftest factor-is-in-range-test
  (testing "should return factor when in range"
    (is (= 1.15 (factor/calculate 115.0)))
    (is (= 0.97 (factor/calculate 97.0)))))