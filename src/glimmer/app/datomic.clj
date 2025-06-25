(ns glimmer.app.datomic
  (:require [glimmer.app.config :as config]
            [glimmer.datomic.connect :as dcon]))

(def client
  (delay
    (let [cfg (config/get)]
      (dcon/get-client cfg))))
