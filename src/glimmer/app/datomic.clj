(ns glimmer.app.datomic
  (:require [glimmer.datomic.connect :as dcon]))

(defn client
  "Best way of acquiring a datomic client based
  on glimmer app config of current environment."
  [{:keys [aws/region
           datomic/system
           datomic/endpoint
           datomic/server-type]}]
  ;; Use our util to get cached client to this system
  (let [opts (merge {:system      system
                     :region      region
                     ;; Default to :dev-local for development
                     :server-type (or server-type :dev-local)}
                    (when endpoint
                      {:endpoint endpoint}))]
    (dcon/get-client opts)))
