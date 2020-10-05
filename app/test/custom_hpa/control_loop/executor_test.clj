(ns custom-hpa.control-loop.executor-test
  (:require [clojure.test :refer :all]
            [custom-hpa.test-helper.kube :refer [kube-fixture with-current-pods deployment-fix deployment-default-spec]]
            [custom-hpa.helpers.env :refer [int-env]]
            [custom-hpa.control-loop.executor :as executor]))

(def ^:private max-replicas (int-env "MAX_REPLICAS"))
(def ^:private min-replicas (int-env "MIN_REPLICAS"))

(use-fixtures :each kube-fixture)

(deftest scale-up-when-current-pods-equal-max-replicas-test
  (with-current-pods max-replicas
    (executor/scale 1.2)
    (testing "should not update deployment spec"
      (is (= max-replicas (:current @deployment-fix)))
      (is (= (:desired deployment-default-spec) (:desired @deployment-fix))))))

(deftest scale-down-when-current-pods-equal-min-replicas
  (with-current-pods min-replicas
    (executor/scale 0.95)
    (testing "should not update deployment spec"
      (is (= min-replicas (:current @deployment-fix)))
      (is (= (:desired deployment-default-spec) (:desired @deployment-fix))))))

(deftest scale-up-to-max-replicas-test
  (executor/scale 2.0)
  (testing "should set desired pods to max replicas"
    (is (= max-replicas (:desired @deployment-fix)))
    (is (= max-replicas (:current @deployment-fix)))))

(deftest scale-down-to-min-replicas-test
  (executor/scale 0.1)
  (testing "should set desired pods to min replicas"
    (is (= min-replicas (:desired @deployment-fix)))
    (is (= min-replicas (:current @deployment-fix)))))

(deftest should-scale
  (executor/scale 1.5)
  (are [k] (= (int (* 1.5 (k deployment-default-spec))) (k @deployment-fix))
           :current
           :desired))