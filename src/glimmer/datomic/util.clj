(ns glimmer.datomic.util
  (:require [datomic.client.api :as d]))

(defn with-retry
  "Execute function f with retry logic"
  [f]
  (let [max-attempts 3]
    (loop [attempt 1]
      (try
        (f)
        (catch Exception e
          (if (< attempt max-attempts)
            (do
              (Thread/sleep (* attempt 1000))
              (recur (inc attempt)))
            (throw e)))))))

(defn expand-schema
  "Expand schema definition into Datomic schema attributes"
  [entity-map add-attrs]
  (for [[entity-name attrs] entity-map
        [attr-name [doc type & opts]] attrs]
    (let [base-attr {:db/ident       (keyword (name entity-name) (name attr-name))
                     :db/valueType   type
                     :db/cardinality :db.cardinality/one
                     :db/doc         doc}]
      (reduce (fn [attr opt]
                (cond
                  (= opt :db/index) (assoc attr :db/index true)
                  (keyword? opt) (assoc attr opt true)
                  (map? opt) (merge attr opt)
                  :else attr))
              base-attr
              opts))))

(defn pull
  "Pull entity data with error handling"
  [db pattern entity-id]
  (try
    (d/pull db pattern entity-id)
    (catch Exception e
      {:error (.getMessage e)})))