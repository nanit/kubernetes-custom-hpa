(ns custom-hpa.metric.protocol.provider)

(defprotocol Provider
  (fetch [this]))
