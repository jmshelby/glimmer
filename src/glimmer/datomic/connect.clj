(ns glimmer.datomic.connect
  (:require [datomic.client.api :as d]
            [glimmer.datomic.util :refer [with-retry]]))

(def get-client
  (memoize (fn [opts]
             (d/client
               (select-keys opts [:server-type
                                  :region
                                  :system
                                  :endpoint
                                  :access-key
                                  :secret])))))

(def get-connection
  (fn [client db-name]
    (with-retry
      #(d/connect client {:db-name db-name}))))