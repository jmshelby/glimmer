(ns local.sandbox
  (:require [datomic.client.api :as d]
            [glimmer.app.datomic :as datomic]
            [glimmer.datomic.connect :as dcon]
            [glimmer.core :as core]
            [glimmer.app.service :as service]))

(require '[local.env])

(comment
  ;; Initialize database and schema
  (def client @datomic/client)  
  (d/create-database client {:db-name "glimmer"})
  (core/init-schema!)
  
  ;; Get connection for testing
  (def conn (dcon/get-connection client "glimmer"))
  
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
    (service/ping-pong-query conn {:ping (:ping/id (get-in ping-result [:out :ping]))})))