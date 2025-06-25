(ns glimmer.app.db.schema
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [glimmer.datomic.api :refer [txact-if]]
            [glimmer.datomic.util :refer [expand-schema]]))

(def SCHEMA-SOURCES
  [{:key      :base
    :resource "app/db/schema/base.edn"}])

(defn- read-resource [p]
  (if-let [r (io/resource p)]
    (edn/read-string (slurp r))
    (throw (ex-info "Unable to get and read resource" {:path p}))))

(defn get-attributes [path add]
  (let [base  (read-resource path)
        elems (:schema base)
        ents  (:entities base [])]
    [(mapcat #(expand-schema % add) elems)
     ents]))

(defn ensure-schema! [conn]
  (mapv
    (fn [{:keys [key add resource]}]
      (let [[schema ents] (get-attributes resource (or add {}))]
        [key
         (update (txact-if conn schema) :tx-data vec)
         (update (txact-if conn ents) :tx-data vec)]))
    SCHEMA-SOURCES))
