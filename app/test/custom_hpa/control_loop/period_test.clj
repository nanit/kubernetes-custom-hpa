(ns custom-hpa.control-loop.period-test
  (:require [clojure.test :refer :all]
            [clj-time.core :as time]
            [custom-hpa.helpers.env :refer [int-env double-env]]
            [custom-hpa.test-helper.kube :refer [kube-fixture deployment-default-spec deployment-fix with-current-pods]]
            [custom-hpa.test-helper.metric.core :as metric :refer [metric-fixture]]
            [custom-hpa.test-helper.control-loop.status :refer [status-fixture]]
            [custom-hpa.control-loop.period :as period]
            [custom-hpa.control-loop.status :as status]
            [custom-hpa.control-loop.common :refer [scale-up scale-down]]))

(use-fixtures :each kube-fixture metric-fixture status-fixture)

(defn- scaled? [scale-type] (status/active-event? scale-type status/scaled))

(defn- run-cooldown-test [scale-type]
  (testing (str "cooldown for scale" (name scale-type))
    (let [now (time/now)]
      (with-redefs [time/now (constantly now)]
        (testing "should not scale when in cooldown"
          (status/scaled scale-type)
          (period/run)
          (is (not (scaled? scale-type))))
        (testing "should report cooldown event"
          (status/active-event? scale-type status/cooldown))
        (testing "last scale time should not change"
          (is (= now (status/last-scale-time scale-type))))))))

(deftest no-metric-sample-test
  (testing "should not scale deployment when no metric fetched"
    (metric/seed-samples [])
    (period/run)
    (is (not (scaled? scale-up)))
    (is (not (scaled? scale-down)))))

(deftest scale-up-in-cooldown-test
  (metric/seed-scale-up-samples 1)
  (run-cooldown-test scale-up))

(deftest scale-down-in-cooldown-test
  (metric/seed-scale-down-samples 1)
  (run-cooldown-test scale-down))

(deftest scale-up-factor-below-minimum-test
  (metric/seed-samples [(metric/sample (/ metric/scale-up-min-sample 2) metric/scale-up-min-sample)])
  (testing "should not scale when factor is below scale up min factor"
    (period/run)
    (is (not (scaled? scale-up))))
  (testing "should report event"
    (status/active-event? scale-up status/below-min-factor)))

(deftest scale-down-factor-below-minimum-test
  (metric/seed-samples [(metric/sample (* metric/scale-down-min-sample 2) metric/scale-up-min-sample)])
  (testing "should not scale when factor is below scale down min factor"
    (period/run)
    (is (not (scaled? scale-down))))
  (testing "should report event"
    (status/active-event? scale-down status/below-min-factor)))

(deftest scale-up-abort-current-pods-equal-max-replicas-test
  (testing "should not scale up when current pods = max replicas"
    (with-current-pods (int-env "MAX_REPLICAS")
      (metric/seed-samples [120.0])
      (period/run)
      (is (not (scaled? scale-up)))))
  (testing "should report event"
    (status/active-event? scale-up status/limited)))

(deftest scale-down-abort-current-pods-equal-min-replicas-test
  (testing "should not scale down when current pods = max replicas"
    (with-current-pods (int-env "MIN_REPLICAS")
      (metric/seed-samples [96.0])
      (period/run)
      (is (not (scaled? scale-up)))))
  (testing "should report event"
    (status/active-event? scale-down status/limited)))

(defn- calc-expected-pods
  [current-pods sample]
  (-> sample
      (/ (double-env "TARGET_VALUE"))
      (* current-pods)
      Math/ceil
      int))

(deftest scale-up-test
  (let [samples (metric/scale-up-samples 3)]
    (metric/seed-samples samples)
    (let [now (time/now)]
      (dotimes [n (count samples)]
        (time/do-at (time/plus now (time/seconds (* (int-env "CONTROL_LOOP_PERIOD") n)))
          (let [current-pods (:current @deployment-fix)
                expected-pods-count (calc-expected-pods current-pods (-> samples seq (nth n)))]
            (period/run)
            (is (scaled? scale-up))
            (is (= expected-pods-count (:current @deployment-fix)))))))))

(deftest scale-down-test
  (let [samples (metric/scale-down-samples 3)]
    (metric/seed-samples samples)
    (let [now (time/now)]
      (dotimes [n (count samples)]
        (time/do-at (time/plus now (time/seconds (* (int-env "CONTROL_LOOP_PERIOD") n)))
          (let [current-pods (:current @deployment-fix)
                expected-pods-count (calc-expected-pods current-pods (-> samples seq (nth n)))]
            (period/run)
            (is (scaled? scale-down))
            (is (= expected-pods-count (:current @deployment-fix)))))))))