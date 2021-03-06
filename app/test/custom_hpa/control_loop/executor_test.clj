(ns custom-hpa.control-loop.executor-test
  (:require [clojure.test :refer :all]
            [custom-hpa.test-helper.kube :as kube :refer [kube-fixture with-current-pods deployment-fix deployment-default-spec]]
            [custom-hpa.helpers.env :refer [int-env]]
            [custom-hpa.control-loop.executor :as executor]))

(def ^:private max-replicas (int-env "MAX_REPLICAS"))
(def ^:private min-replicas (int-env "MIN_REPLICAS"))
(def ^:private deployment (System/getenv "DEPLOYMENT"))
(def ^:private deployment-namespace (System/getenv "NAMESPACE"))

(use-fixtures :each kube-fixture)

(deftest scale-up-to-max-replicas-test
  (executor/scale kube/dummy-client deployment deployment-namespace 2.0)
  (testing "should set desired pods to max replicas"
    (is (= max-replicas (:desired @deployment-fix)))
    (is (= max-replicas (:current @deployment-fix)))))

(deftest scale-down-to-min-replicas-test
  (executor/scale kube/dummy-client deployment deployment-namespace 0.1)
  (testing "should set desired pods to min replicas"
    (is (= min-replicas (:desired @deployment-fix)))
    (is (= min-replicas (:current @deployment-fix)))))

(deftest should-scale
  (executor/scale kube/dummy-client deployment deployment-namespace 1.5)
  (are [k] (= (int (* 1.5 (k deployment-default-spec))) (k @deployment-fix))
           :current
           :desired))