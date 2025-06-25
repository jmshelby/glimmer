(ns glimmer.app.api
  (:require [ring.util.response :as ring]
            [glimmer.app.service :as service]
            [glimmer.datomic.connect.pedestal :as dcon]))

(defn handle-operation
  [{!               ::dcon/conn
    {:keys [op in]} :json-params}]
  (let [out
        (try
          (case op
            "ping"            (service/ping-create ! in)
            "ping/query"      (service/ping-query ! in)
            "pong"            (service/pong-create ! in)
            "ping.pong/query" (service/ping-pong-query ! in)
            {:anomaly :unknown-operation :op op})
          (catch Exception e
            {:anomaly (.getMessage e)}))]
    (ring/response out)))

(def ceptors (into [] dcon/chain))

(def routes #{["/glimmer/op" :any (conj ceptors `handle-operation)]})