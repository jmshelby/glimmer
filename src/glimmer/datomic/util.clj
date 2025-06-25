(ns glimmer.datomic.util
  (:require [datomic.client.api :as d]
            [cognitect.anomalies :as anomalies]))

(def retryable-anomaly?
  "Set of retryable anomalies."
  #{::anomalies/busy
    ::anomalies/unavailable
    ::anomalies/interrupted})

(defn with-retry
  "Try op, return result if successful, if op throws, check exception against retry? pred,
  retrying if the predicate returns true. Optional backoff function controls retry wait. Backoff
  function should return msec backoff desired or nil to stop retry.
  Defaults to trying 10 times with linear backoff."
  [op & {:keys [retry? backoff]
         :or   {retry?  (fn [e]
                          (-> e ex-data ::anomalies/category retryable-anomaly?))
                backoff (fn [epoch]
                          (when (<= epoch 10)
                            (* 200 epoch)))}}]
  (loop [epoch 1]
    (let [[success val] (try [true (op)]
                             (catch Exception e
                               [false e]))]
      (if success
        val
        (if-let [ms (and (retry? val) (backoff epoch))]
          (do
            (Thread/sleep ms)
            (recur (inc epoch)))
          (throw val))))))

;; Expressive Schema

(defn- some-ns
  ([coll s]
   (some-ns coll s nil))
  ([coll s default]
   (or (some #(and (keyword? %) (= (namespace %) s) %) coll)
       default)))

(defn- make-attr [ns [attr props] add]
  (let [maps (apply merge (filter map? props))
        docs (seq (filter string? props))
        comp (some #{:db/isComponent} props)
        hist (some #{:db/noHistory} props)
        uniq (some-ns props "db.unique")
        type (some-ns props "db.type")
        card (when type
               (some-ns props "db.cardinality" :db.cardinality/one))]

    (cond-> #:db{:ident
                 (keyword (name ns) (name attr))}
      type    (assoc :db/valueType type)
      card    (assoc :db/cardinality card)
      uniq    (assoc :db/unique uniq)
      comp    (assoc :db/isComponent (boolean comp))
      hist    (assoc :db/noHistory (boolean hist))
      docs    (assoc :db/doc (apply str (interpose " " docs)))
      maps    (merge maps)
      :always (merge add))))

(defn expand-schema
  "Given a map of namespaces -> concise property maps, expand into flat
  list of datomic, transact ready, schema maps.
  'property map' being names -> set of datomic schema values.
  When type is provided, element is assumed to be an attr, and provides defaults:
    cardinality: one; (and other built in defaults)
  When type is not provided, element is assumed to be ident only (for use as ref, like for enum values)."
  ([m] (expand-schema m {}))
  ([m add]
   (letfn [(exp [[ns spec]]
             (map #(make-attr ns % add) spec))]
     (mapcat exp m))))

(defn pull
  ([db eid] (pull db '[*] eid))
  ([db pattern eid] (d/pull db pattern eid)))

(defn entity-exists? [db ident]
  (-> db
      (pull [:db/id] ident)
      count
      (> 1)))