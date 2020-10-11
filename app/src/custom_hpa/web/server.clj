(ns custom-hpa.web.server
  (:require [taoensso.timbre :as logger]
            [compojure.core :refer [defroutes GET]]
            [org.httpkit.server :as http-kit]
            [custom-hpa.monitor.prometheus :as prometheus]))

(defonce ^:private server (atom nil))

(defroutes app-routes
  (GET "/ping" [] {:status 200 :body "pong"})
  (GET "/metrics" [] {:status 200 :body (prometheus/export)}))

(defn- stop-server []
  (@server :timeout 5000)
  (reset! server nil))

(defn start [port]
  (logger/debug "Starting web server")
  (.addShutdownHook (Runtime/getRuntime) (Thread. stop-server))
  (reset! server (http-kit/run-server app-routes {:port port})))

