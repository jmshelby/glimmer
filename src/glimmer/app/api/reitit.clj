(ns glimmer.app.api.reitit
  (:require [reitit.ring :as ring]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [reitit.coercion.malli :as malli]
            [reitit.ring.coercion :as coercion]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]
            [muuntaja.core :as m]
            [glimmer.app.service :as service]
            [glimmer.util :as util]))

;; Malli schemas for request/response validation
(def CoordinatesSchema
  [:vector {:min 2 :max 2} number?])

(def PingCreateRequest
  [:map
   [:coords CoordinatesSchema]
   [:tag string?]])

(def PingCreateResponse
  [:map
   [:status [:enum :created]]
   [:ping [:map
           [:id string?]
           [:coords CoordinatesSchema]
           [:tag string?]
           [:created string?]]]])

(def PingQueryRequest
  [:map
   [:tag {:optional true} string?]
   [:limit {:optional true} pos-int?]])

(def PingQueryResponse
  [:map
   [:status [:enum :ok]]
   [:pings [:vector [:map
                     [:id string?]
                     [:coords CoordinatesSchema]
                     [:tag string?]
                     [:created string?]]]]])

(def PongCreateRequest
  [:map
   [:pings [:vector string?]]
   [:source {:optional true} string?]])

(def PongCreateResponse
  [:map
   [:status [:enum :created]]
   [:count nat-int?]])

(def PongQueryRequest
  [:map
   [:ping string?]])

(def PongQueryResponse
  [:map
   [:status [:enum :ok]]
   [:pongs [:vector [:map
                     [:id string?]
                     [:source {:optional true} string?]
                     [:created string?]]]]])

;; Helper to get Datomic connection from request context
(defn get-connection [request]
  (get-in request [:glimmer.datomic.connect.pedestal/conn]))

;; Transform ping data from datomic format to API format
(defn transform-ping [ping]
  (-> ping
      (assoc :coords [(:ping/lat ping) (:ping/lng ping)])
      (assoc :id (str (:ping/id ping)))
      (assoc :tag (:ping/tag ping))
      (assoc :created (str (:entity.date/created ping)))
      (select-keys [:id :coords :tag :created])))

;; Transform pong data from datomic format to API format  
(defn transform-pong [pong]
  (-> pong
      (assoc :id (str (:pong/id pong)))
      (assoc :created (str (:entity.date/created pong)))
      (cond-> (:pong/source pong) (assoc :source (:pong/source pong)))
      (select-keys [:id :source :created])))

;; Route handlers
(defn ping-create-handler [{{:keys [coords tag]} :body-params :as request}]
  (let [conn (get-connection request)
        result (service/ping-create conn {:coords coords :tag tag})
        out (:out result)]
    {:status 200
     :body (-> out
               (update :ping transform-ping))}))

(defn ping-query-handler [{{:keys [tag limit]} :body-params :as request}]
  (let [conn (get-connection request)
        params (cond-> {}
                 tag (assoc :tag tag)
                 limit (assoc :limit limit))
        result (service/ping-query conn params)
        out (:out result)]
    {:status 200
     :body (-> out
               (update :pings #(map transform-ping %)))}))

(defn pong-create-handler [{{:keys [pings source]} :body-params :as request}]
  (let [conn (get-connection request)
        params (cond-> {:pings pings}
                 source (assoc :source source))
        result (service/pong-create conn params)
        out (:out result)]
    {:status 200
     :body out}))

(defn pong-query-handler [{{:keys [ping]} :body-params :as request}]
  (let [conn (get-connection request)
        result (service/ping-pong-query conn {:ping ping})
        out (:out result)]
    {:status 200
     :body (-> out
               (update :pongs #(map transform-pong %)))}))

;; Reitit routes with OpenAPI specs
(def routes
  ["/api"
   {:swagger {:tags ["glimmer"]}}
   
   ["/swagger.json"
    {:get {:no-doc true
           :swagger {:info {:title "Glimmer API"
                           :description "Anonymous location-based ping/pong API for sharing presence and acknowledgments"
                           :version "1.0.0"}}
           :handler (swagger/create-swagger-handler)}}]
   
   ["/ping"
    {:post {:summary "Create a ping"
            :description "Submit your location when feeling invisible, lonely, or need help"
            :parameters {:body PingCreateRequest}
            :responses {200 {:body PingCreateResponse}}
            :handler ping-create-handler}}]
   
   ["/ping/query"
    {:post {:summary "Query pings"
            :description "Get recent pings, optionally filtered by tag"
            :parameters {:body PingQueryRequest}
            :responses {200 {:body PingQueryResponse}}
            :handler ping-query-handler}}]
   
   ["/pong"
    {:post {:summary "Create pongs (acknowledge pings)"
            :description "Acknowledge multiple pings at once to show support"
            :parameters {:body PongCreateRequest}
            :responses {200 {:body PongCreateResponse}}
            :handler pong-create-handler}}]
   
   ["/pong/query"
    {:post {:summary "Query pongs for a ping"
            :description "See who acknowledged a specific ping"
            :parameters {:body PongQueryRequest}
            :responses {200 {:body PongQueryResponse}}
            :handler pong-query-handler}}]])

;; Create the reitit router
(def router
  (ring/router
    [routes
     ;; Swagger UI
     ["/docs/*" {:get {:no-doc true
                       :handler (swagger-ui/create-swagger-ui-handler
                                  {:url "/api/swagger.json"
                                   :config {:validatorUrl nil}})}}]]
    {:data {:coercion malli/coercion
            :muuntaja m/instance
            :middleware [;; swagger feature
                         swagger/swagger-feature
                         ;; query-params & form-params
                         parameters/parameters-middleware
                         ;; content-negotiation
                         muuntaja/format-negotiate-middleware
                         ;; encoding response body
                         muuntaja/format-response-middleware
                         ;; exception handling
                         coercion/coerce-exceptions-middleware
                         ;; decoding request body
                         muuntaja/format-request-middleware
                         ;; coercing response bodys
                         coercion/coerce-response-middleware
                         ;; coercing request parameters
                         coercion/coerce-request-middleware]}}))

;; Ring handler
(def handler
  (ring/ring-handler router))