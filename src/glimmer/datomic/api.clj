(ns glimmer.datomic.api
  (:require [datomic.client.api :as d]))

(defn txact-if
  "Transact data only if not empty, return result with tx info"
  [conn tx-data]
  (if (seq tx-data)
    (d/transact conn {:tx-data tx-data})
    {:db-before (d/db conn)
     :db-after  (d/db conn)
     :tx-data   []
     :tempids   {}}))

(defn db
  "Get current database value"
  [conn]
  (d/db conn))
