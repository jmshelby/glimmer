(ns local.sandbox
  (:require [datomic.client.api :as d]
            [glimmer.app.datomic :as datomic]
            [glimmer.datomic.connect :as dcon]
            [glimmer.core :as core]
            [glimmer.app.service :as service]))

(require '[local.env])

(comment
  ;; Initialize database and schema
  (require '[glimmer.app.config :as config])

  (def cfg (config/get))

  (def client (datomic/client cfg))

  cfg

  (d/list-databases client {})

  (d/create-database client {:db-name (:datomic/db-name cfg)})

  (core/init-schema!)

  ;; Get connection for testing
  (def conn (dcon/get-connection client (:datomic/db-name cfg)))



  (d/q '[:find ?ident ?type ?cardinality ?doc
         :where
         [?e :db/ident ?ident]
         [?e :db/valueType ?type]
         [?e :db/cardinality ?cardinality]
         [(get-else $ ?e :db/doc "") ?doc]]
       (d/db conn)
       )

  (d/q '[:find ?ident ?type ?cardinality ?doc
         :where
         [?e :db/ident ?ident]
         [?e :db/valueType ?type]
         [?e :db/cardinality ?cardinality]
         [(get-else $ ?e :db/doc "") ?doc]
         [(namespace ?ident) ?ns]
         [(not= ?ns "db")]]
       (d/db conn))


  ;; Test ping creation
  (def ping-result
    (service/ping-create conn {:coords [40.7128 -74.0060]
                               :tag "invisible"}))

  ;; Test ping query
  (def query-result
    (service/ping-query conn {:tag "invisible" :limit 10}))

  ;; Test pong creation
  (def pong-result
    (service/pong-create conn {:pings [(:ping/id (get-in ping-result [:out :ping]))]
                               :source "test-user"}))

  ;; Test pong query
  (def pong-query-result
    (service/ping-pong-query conn {:ping (:ping/id (get-in ping-result [:out :ping]))}))

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
         (d/db conn)))

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

  )
