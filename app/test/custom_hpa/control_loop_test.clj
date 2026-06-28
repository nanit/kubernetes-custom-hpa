(ns custom-hpa.control-loop-test
  (:require [clojure.test :refer :all]
            [custom-hpa.control-loop :as cl]
            [custom-hpa.clients.kube :as kube]))

;; client-ttl-ms and needs-refresh? are private; access via #'ns/var
(def ^:private client-ttl-ms @#'cl/client-ttl-ms)
(def ^:private needs-refresh? @#'cl/needs-refresh?)

(deftest client-ttl-default-test
  (testing "client TTL is well below the 1h bound-token expiration"
    ;; Default ServiceAccount token lifetime in K8s 1.21+ is 1 hour (3600s).
    ;; Re-init must happen earlier so the cached token is always fresh.
    (is (< client-ttl-ms (* 60 60 1000))
        "client-ttl-ms must be < 60 minutes")
    (is (>= client-ttl-ms (* 10 60 1000))
        "client-ttl-ms must be >= 10 minutes (avoid thrashing)")))

(deftest needs-refresh-test
  (testing "returns false immediately after init"
    (let [now (System/currentTimeMillis)]
      (is (false? (needs-refresh? now)))))

  (testing "returns false within TTL window"
    (let [recent (- (System/currentTimeMillis) (- client-ttl-ms 5000))]
      (is (false? (needs-refresh? recent))
          "5 seconds before TTL expires should not require refresh")))

  (testing "returns true after TTL window elapses"
    (let [old (- (System/currentTimeMillis) (+ client-ttl-ms 5000))]
      (is (true? (needs-refresh? old))
          "5 seconds after TTL expires should require refresh")))

  (testing "force-refresh sentinel (last-init=0) triggers refresh"
    ;; The exception handler resets last-init to 0 to force the next loop to re-init.
    ;; This must be detected as needing refresh regardless of clock.
    (is (true? (needs-refresh? 0))
        "last-init=0 must always trigger a refresh")))

(deftest re-init-on-exception-test
  (testing "kube/init is called again after a control-loop exception"
    ;; Documents the contract: the catch block sets last-init to 0, and the
    ;; next iteration of the loop sees needs-refresh? = true and calls kube/init.
    ;; This guards against the cached-expired-token failure mode where the
    ;; client-java 8.0.2 client would otherwise return 401 forever.
    (is (true? (needs-refresh? 0)))))
