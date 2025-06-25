(ns glimmer.core
  (:require [glimmer.app.ion :as ion]
            [glimmer.app.db.schema :as schema]
            [glimmer.app.datomic :as datomic]
            [glimmer.datomic.connect :as dcon]))

(defn init-schema!
  "Initialize database schema"
  []
  (let [client @datomic/client
        db-name "glimmer"
        conn (dcon/get-connection client db-name)]
    (schema/ensure-schema! conn)))

(def handler ion/handler)