(ns glimmer.datomic.connect.pedestal
  (:require [datomic.client.api :as d]
            [io.pedestal.interceptor :as interceptor]
            [glimmer.app.config :as config]
            [glimmer.app.datomic :refer [client]]
            [glimmer.datomic.connect :as dcon]))

(def param-get-cept
  (interceptor/interceptor
    {:name  ::param-get-cept
     :enter (fn [ctx]
              (let [config (config/get)]
                (-> ctx
                    (assoc ::params config)
                    (assoc-in [:request ::params] config))))}))

(def client-cept
  (interceptor/interceptor
    {:name  ::client-cept
     :enter (fn [ctx]
              (-> ctx
                  (assoc ::client @client)
                  (assoc-in [:request ::client] @client)))}))

(def conn-cept
  (interceptor/interceptor
    {:name  ::conn-interceptor
     :enter (fn [ctx]
              (let [dbname (get-in ctx [::params :datomic/db-name])
                    conn   (dcon/get-connection (::client ctx) dbname)
                    m      {::conn conn
                            ::db   (d/db conn)}]
                (-> ctx
                    (merge m)
                    (update-in [:request] merge m))))}))

(def chain
  [param-get-cept
   client-cept
   conn-cept])
