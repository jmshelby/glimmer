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
  "Get base params from AWS SSM for the current env & app pair."
  ([] (get-cloud-params (env) (app)))
  ([env app]
   (when (not (or env app))
     (throw (ex-info "Glimmer app param configuration not setup, are you running this app with the correct configurations?"
                     {:configuration {:env env
                                      :app app}})))
   (->> (ion/get-params
          {:path (format "/datomic-shared/%s/%s/" (name env) app)})
        (map #(vector (keyword (first %)) (second %)))
        (into {}))))

(defn get-params
  "Current in-memory environment params, for this app" []
  (when (= ::empty @STATE)
    ;; Lazy load params when state is empty
    ;; with existing env and app name
    (clojure.core/reset! STATE (get-cloud-params)))
  @STATE)

(defn get
  "Main method to get configuration parameters"
  ([] (get-params))
  ([key] (clojure.core/get (get-params) key)))

