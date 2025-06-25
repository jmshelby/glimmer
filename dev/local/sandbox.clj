(ns local.sandbox
  "A place to play around with the code and mess with the database"
  (:require [datomic.client.api :as d]
            [glimmer.app.datomic :as datomic]
            [glimmer.datomic.connect :as dcon]
            [glimmer.core :as core]
            [glimmer.app.service :as service]
            [glimmer.app.config :as config]
            [glimmer.datomic.util :as du]
            [glimmer.app.db.schema :as schema]))

(require '[local.env])

(declare db stats insert retract
         pull fetch retire-ident
         attrs switch conn client)

(comment

  (config/get)

  (get-env-info)

  (conn)



  (def cfg (config/get))

  (def client (datomic/client cfg))

  cfg

  (d/list-databases client {})

  (d/create-database client {:db-name (:datomic/db-name cfg)})

  (core/init-schema!)

  ;; Get connection for testing
  (def conn (dcon/get-connection client (:datomic/db-name cfg)))

  ;; Query all attributes in the system
  (def all-attributes
    (d/q '[:find ?ident ?type ?cardinality ?doc
           :where
           [?e :db/ident ?ident]
           [?e :db/valueType ?type]
           [?e :db/cardinality ?cardinality]
           [(get-else $ ?e :db/doc "") ?doc]]
         (d/db conn)))

  ;; Query just our custom attributes (non-db namespace)
  (def custom-attributes
    (d/q '[:find ?ident ?type ?cardinality ?doc
           :where
           [?e :db/ident ?ident]
           [?e :db/valueType ?type]
           [?e :db/cardinality ?cardinality]
           [(get-else $ ?e :db/doc "") ?doc]
           [(namespace ?ident) ?ns]
           [(not= ?ns "db")]]
         (d/db conn))

    )

  ;; Pretty print our custom attributes
  (doseq [[ident type card doc] (sort custom-attributes)]
    (println (format "%-25s %-20s %-15s %s" ident type card doc)))

  ;; === PULL SYNTAX VERSIONS ===

  ;; Query all attributes in the system using pull
  (def all-attributes-pull
    (d/q '[:find (pull ?e [:db/ident :db/valueType :db/cardinality :db/doc])
           :where
           [?e :db/ident]
           [?e :db/valueType]]
         (d/db conn)))

  ;; Query just our custom attributes (non-db namespace) using pull
  (def custom-attributes-pull
    (d/q '[:find (pull ?e [:db/ident :db/valueType :db/cardinality :db/doc])
           :where
           [?e :db/ident ?ident]
           [?e :db/valueType]
           [(namespace ?ident) ?ns]
           [(not= ?ns "db")]]
         (d/db conn))

    )

  ;; Pretty print custom attributes from pull results
  (doseq [attr-map (->> custom-attributes-pull
                        (map first)
                        (sort-by :db/ident))]
    (println (format "%-25s %-20s %-15s %s"
                     (:db/ident attr-map)
                     (:db/valueType attr-map)
                     (:db/cardinality attr-map)
                     (or (:db/doc attr-map) ""))))

  ;; Alternative: Get all schema attributes as a collection of maps
  (def schema-attrs
    (->> (d/q '[:find ?e
                :where
                [?e :db/ident ?ident]
                [?e :db/valueType]
                [(namespace ?ident) ?ns]
                [(not= ?ns "db")]]
              (d/db conn))
         (map #(d/pull (d/db conn) [:db/ident :db/valueType :db/cardinality :db/doc] (first %)))
         (sort-by :db/ident))

    )



  ;; Test ping creation
  (def ping-result
    (service/ping-create conn {:coords [40.7128 -74.0060]
                               :tag    "invisible"}))

  ;; Test ping query
  (def query-result
    (service/ping-query conn {:tag "invisible" :limit 10}))

  ;; Test pong creation
  (def pong-result
    (service/pong-create conn {:pings  [(:ping/id (get-in ping-result [:out :ping]))]
                               :source "test-user"}))

  ;; Test pong query
  (def pong-query-result
    (service/ping-pong-query conn {:ping (:ping/id (get-in ping-result [:out :ping]))}))

  )

;; ===== UTILITY FUNCTIONS =====
;; These provide convenient REPL functions similar to huckleberry
;; Usage:
;;   (switch :local)  ; or (switch :cloud)
;;   (client)         ; get datomic client
;;   (conn)           ; get connection
;;   (db)             ; get database value
;;   (stats)          ; database statistics
;;   (attrs)          ; list all schema attributes
;;   (insert [{:ping/id (random-uuid) ...}])  ; insert data
;;   (fetch [:ping/tag "invisible"])           ; find by attr/val
;;   (pull some-entity-id)                     ; pull entity data

(declare get-env-info)

(defn client [] (dcon/get-client (:client (get-env-info))))
(defn conn [] (dcon/get-connection (client) (:db-name (get-env-info))))
(defn db [] (d/db (conn)))
(defn stats [] (d/db-stats (db)))
(defn insert [recs] (d/transact (conn) {:tx-data recs}))
(defn retract [ids] (let [pairs (map #(vector :db/retractEntity %) ids)]
                      (d/transact (conn) {:tx-data pairs})))

;; Useful for needing to redeclare an attribute in a type breaking way
(defn retire-ident [ident reason]
  (let [new-ns    (str "deprecated." (namespace ident))
        new-name  (str (name ident) "_" reason)
        new-ident (keyword new-ns new-name)]
    (insert [{:db/id    ident
              :db/ident new-ident}])))

(defn attrs []
  ;; Get all schema attributes
  (->> (d/q '[:find (pull ?e [*])
              :where [?e :db/ident]
              [?e :db/valueType]]
            (db))
       (map first)
       (map :db/ident)))

(defn pull
  ([eid] (pull '[*] eid))
  ([pattern eid] (d/pull (db) pattern eid)))

(defn fetch
  ([[attr val]]
   (fetch [attr val] '[*]))
  ([[attr val] pexp]
   (map first
        (if val
          (d/q '[:find (pull ?e pexp)
                 :in $ ?attr ?val pexp
                 :where [?e ?attr ?val]] (db) attr val pexp)
          (d/q '[:find (pull ?e pexp)
                 :in $ ?attr pexp
                 :where [?e ?attr]] (db) attr pexp)))))

(defonce current-env (atom :local))
(defn switch [env]
  (reset! current-env env))

(defn get-env-info []
  (case @current-env
    :local {:db-name (config/get :datomic/db-name)
            :client  {:system      (config/get :datomic/system)
                      :server-type (config/get :datomic/server-type)
                      :region      (config/get :aws/region)}}
    :cloud {:db-name "glimmer.core.prod"
            :client  {:system      "glimmer-core-prod"
                      :server-type :ion
                      :endpoint    "https://your-endpoint.execute-api.region.amazonaws.com"
                      :region      "us-east-1"}}
    :else  (throw (Exception. (str "Unknown env name" @current-env)))))
