(ns glimmer.app.rest
  (:require [io.pedestal.http.route :as route]
            [glimmer.app.service :as service]
            [glimmer.datomic.connect.pedestal :as dcon-ped]))

;; Transform ping data from datomic format to REST API format
(defn transform-ping [ping]
  (-> ping
      (assoc :coords [(:ping/lat ping) (:ping/lng ping)])
      (assoc :id (str (:ping/id ping)))
      (assoc :tag (:ping/tag ping))
      (assoc :created (str (:entity.date/created ping)))
      (select-keys [:id :coords :tag :created])))

;; Transform pong data from datomic format to REST API format  
(defn transform-pong [pong]
  (-> pong
      (assoc :id (str (:pong/id pong)))
      (assoc :created (str (:entity.date/created pong)))
      (cond-> (:pong/source pong) (assoc :source (:pong/source pong)))
      (select-keys [:id :source :created])))

;; REST API handlers
(defn ping-create-handler [request]
  (let [conn (::dcon-ped/conn request)
        body (:json-params request)
        {:keys [coords tag]} body
        result (service/ping-create conn {:coords coords :tag tag})
        out (:out result)]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (-> out
               (update :ping transform-ping))}))

(defn ping-query-handler [request]
  (let [conn (::dcon-ped/conn request)
        body (:json-params request)
        {:keys [tag limit]} body
        params (cond-> {}
                 tag (assoc :tag tag)
                 limit (assoc :limit limit))
        result (service/ping-query conn params)
        out (:out result)]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (-> out
               (update :pings #(map transform-ping %)))}))

(defn pong-create-handler [request]
  (let [conn (::dcon-ped/conn request)
        body (:json-params request)
        {:keys [pings source]} body
        params (cond-> {:pings pings}
                 source (assoc :source source))
        result (service/pong-create conn params)
        out (:out result)]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body out}))

(defn pong-query-handler [request]
  (let [conn (::dcon-ped/conn request)
        body (:json-params request)
        {:keys [ping]} body
        result (service/ping-pong-query conn {:ping ping})
        out (:out result)]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (-> out
               (update :pongs #(map transform-pong %)))}))

;; Pedestal routes for REST API  
(def routes
  #{["/api/ping" :post (conj dcon-ped/chain ping-create-handler) :route-name :api-ping-create]
    ["/api/ping/query" :post (conj dcon-ped/chain ping-query-handler) :route-name :api-ping-query]
    ["/api/pong" :post (conj dcon-ped/chain pong-create-handler) :route-name :api-pong-create]
    ["/api/pong/query" :post (conj dcon-ped/chain pong-query-handler) :route-name :api-pong-query]})