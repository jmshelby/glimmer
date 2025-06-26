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

  (d/list-databases (client) {})

  (d/create-database client {:db-name (:datomic/db-name (config/get))})

  (core/init-schema!)


  (attrs)

  (fetch [:ping/id])

  ;; Test ping creation
  (service/ping-create (conn) {:coords [39.6403, -104.8608]
                               :tag    "jake"})

  ;; Test ping query
  (service/ping-query (conn) {:tag "jake" :limit 10})

  (->>
    (service/ping-query (conn) {:tag "jake" :limit 10})
    :out
    :pings
    (map :ping/id)
    (map str)
    )

  ;; Test pong creation
  ;; Then create pong referencing that ping
  (service/pong-create (conn) {:pings  ["a403b414-5874-491e-8de6-6544bb50154d"
                                        "62b13103-41d5-4ead-a12f-c266421c7f7f"]
                               :source "other-user-4"})

  (->>
    (fetch [:pong/id])
    (sort-by :entity.date/created)
    )

  ;; Test pong query
  (service/ping-pong-query (conn) {:ping
                                   "a403b414-5874-491e-8de6-6544bb50154d"
                                   ;; "27392da9-dfbc-4ad3-9782-b9a4bbda1a0e"
                                   ;; "eca59a91-2b3e-46f8-a2d3-60fcb9a30960"
                                   ;; "62b13103-41d5-4ead-a12f-c266421c7f7f"
                                   })


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
