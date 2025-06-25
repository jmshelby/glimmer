(ns glimmer.core
  (:require [glimmer.app.ion :as ion]
            [glimmer.app.db.schema :as schema]
            [glimmer.app.datomic :as datomic]
            [glimmer.app.config :as config]
            [glimmer.datomic.connect :as dcon]))

(defn init-schema!
  "Initialize database schema"
  []
  (let [cfg    (config/get)
        client (datomic/client cfg)
        db-name (:datomic/db-name cfg)
        conn (dcon/get-connection client db-name)]
    (schema/ensure-schema! conn)))

(def handler ion/handler)
