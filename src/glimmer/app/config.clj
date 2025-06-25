(ns glimmer.app.config
  (:require [glimmer.util :refer [ensure-keyword]]
            [datomic.ion :as ion])
  (:refer-clojure :exclude [get reset!]))

(def STATE (atom ::empty))

(defn reset! []
  (clojure.core/reset! STATE ::empty))

(def app
  (memoize #(or (-> (ion/get-app-info) :app-name)
                (System/getProperty "glimmer.app.ion.app-name"))))

(def env
  (memoize #(or (-> (ion/get-env) :env)
                (-> (System/getProperty "glimmer.app.ion.env")
                    ensure-keyword))))

(defn get-cloud-params
  ([] (get-cloud-params (env) (app)))
  ([env app]
   (->> (ion/get-params
          {:path (format "/datomic-shared/%s/%s/" (name env) app)})
        (map #(vector (keyword (first %)) (second %)))
        (into {}))))

(defn get
  ([] (when (= ::empty @STATE)
        (clojure.core/reset! STATE (get-cloud-params)))
      @STATE)
  ([key] (clojure.core/get (get) key)))

