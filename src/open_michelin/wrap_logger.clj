(ns open-michelin.wrap_logger
  (:require [clojure.string :as s]))

(defn wrap-logger [handler]
  (fn [req]
    (when (not (= "/__source_changed" (:uri req)))
      (println (str
                 (.toUpperCase (name (:request-method req)))
                 " "
                 (name (:scheme req))
                 "//"
                 (:server-name req)
                 ":"
                 (:server-port req)
                 (:uri req)
                 " "
                 (s/join " " (map (fn [[k v]] (str (name k) ":" v)) (:params req)))
                 )))
    (handler req)))
