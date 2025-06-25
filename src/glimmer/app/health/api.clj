(ns glimmer.app.health.api
  (:require [ring.util.response :as ring]))

(defn health-check
  [_]
  (ring/response {:status :ok
                  :service :glimmer
                  :timestamp (java.util.Date.)}))

(def routes #{["/health" :get `health-check]})
