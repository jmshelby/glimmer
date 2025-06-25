(ns glimmer.app.ion
  (:require [clojure.set :refer [union]]
            [io.pedestal.ions :as prov]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            [glimmer.app.api :as app-api]
            [glimmer.app.health.api :as health-api]))

(defn expand-routes []
  (route/expand-routes (union (deref #'app-api/routes)
                              (deref #'health-api/routes))))

(def service {::http/routes (expand-routes)})

(defn make-handler [service-map]
  (-> service-map
      (assoc ::http/chain-provider prov/ion-provider)
      http/default-interceptors
      (update ::http/interceptors conj http/json-body)
      (update ::http/interceptors conj
              (body-params/body-params
                (body-params/default-parser-map)))
      http/create-provider))

(def handler
  "Datomic Ion Web Handler fn"
  (make-handler service))