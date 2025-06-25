(ns local.server
  (:require [io.pedestal.http :as server]
            [io.pedestal.http.body-params :as body-params]
            [glimmer.app.ion :as app]))

(require '[local.env])

(def service
  (merge app/service
         {:env :dev
          ::server/join? false
          ::server/type  :jetty
          ::server/routes app/expand-routes}))

(defn run-dev
  ([] (run-dev 8080))
  ([port]
   (-> service
       (merge {::server/port port
               ::server/host "0.0.0.0"})
       server/default-interceptors
       (update ::server/interceptors conj server/json-body)
       (update ::server/interceptors conj
               (body-params/body-params
                 (body-params/default-parser-map)))
       server/dev-interceptors
       server/create-server
       server/start)))

(defn stop-dev [server]
  (server/stop server))

(comment
  ;; Start development server
  (def dev-server (run-dev 8080))

  ;; Stop development server
  (stop-dev dev-server))
