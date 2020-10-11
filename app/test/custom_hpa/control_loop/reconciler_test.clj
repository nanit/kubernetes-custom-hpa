(ns custom-hpa.control-loop.reconciler-test
  (:require [clojure.test :refer :all]
            [custom-hpa.test-helper.control-loop.status :refer [status-fixture]]
            [custom-hpa.helpers.env :refer [double-env]]
            [custom-hpa.control-loop.common :refer [scale-up scale-down]]
            [custom-hpa.control-loop.status :as status]
            [custom-hpa.control-loop.reconciler :as reconciler]))

(use-fixtures :each status-fixture)

(deftest scale-up-not-allowed-in-cooldown-test
  (testing "should return false"
    (status/scaled scale-up)
    (is (false? (reconciler/scale-allowed? 1.1)))))

(deftest scale-down-not-allowed-in-cooldown-test
  (testing "should return false"
    (status/scaled scale-down)
    (is (false? (reconciler/scale-allowed? 0.96)))))

(deftest scale-up-not-allowed-when-factor-below-min-factor-test
  (testing "should return false"
    (is (false? (reconciler/scale-allowed? 1.05)))))

(deftest scale-down-not-allowed-when-factor-below-min-factor-test
  (testing "should return false"
    (is (false? (reconciler/scale-allowed? 0.995)))))

(deftest scale-up-allowed-test
  (testing "should return true"
    (is (true? (reconciler/scale-allowed? 1.15)))))

(deftest scale-down-allowed-test
  (testing "should return true"
    (is (true? (reconciler/scale-allowed? 0.97)))))