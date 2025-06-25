(ns local.env
  (:require [glimmer.app.config :as config]))

;; Local development configuration
(when (= :dev (config/env))
  (reset! config/STATE
    {:datomic/db-name "glimmer"
     :server-type :dev-local
     :system "dev"}))

;; Create database if it doesn't exist
(comment
  (require '[datomic.client.api :as d])
  (require '[glimmer.app.datomic :as datomic])
  
  (def client @datomic/client)
  (d/create-database client {:db-name "glimmer"}))