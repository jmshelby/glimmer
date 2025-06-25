(ns local.env
  "Special namespace for local development.
  Tries to intelligently determine custom environment configurations."
  (:require [clojure.string :as s]
            [glimmer.app.config :as config]
            [datomic.ion.cast :as cast]))

;; For development purposes, direct ion logging to stderr
(cast/initialize-redirect :stderr)

;; Because of some strange issue with dev-local,
;; to get cast to actually work, we need to log
;; something out before requiring dev-local
(cast/dev {:msg "datomic cast: init"})

(defn get-overrides []
  (merge
    {}
    ;; Check to see if we should override the DB name,
    ;; And if we should use datomic dev local
    (if-let [local (System/getenv "DATOMIC_DEV_LOCAL")]
      ;; TODO - allow pointing to different system name?
      (merge {;; Signals that we'll override to dev-local db
              :datomic/server-type :datomic-local
              ;; We have to do this as long as the main cloud dev system
              ;; has a different name from the typical dev-local setup.
              :datomic/system      "glimmer-dev"}
             (cond (or (= local "true")
                       (= local "1"))
                   {:datomic/db-name "glimmer.local"}
                   (not (s/blank? local))
                   {:datomic/db-name local}))
      ;; Default dev configuration
      {:datomic/db-name     "glimmer"
       :datomic/server-type :datomic-local
       :datomic/system      "dev"})))

(defn inject-local-config! []
  (reset! config/STATE (get-overrides))

  ;; Old logic ...
  ;; Reset every time
  ;; (config/reset!)
  ;; Ensure we have an initial config loaded
  ;; (config/get)
  ;; TODO - check for dev-local to print a message that we're overriding it
  ;; Override certain keys with local preferences
  ;; (swap! config/STATE merge (get-overrides))
  )

;; !!!! This Namespace is meant to invoke an injection into the App Configuration !!!!
(inject-local-config!)

;; Create database if it doesn't exist
(comment
  (require '[datomic.client.api :as d])

  (require '[glimmer.app.datomic :as datomic])

  (def client @datomic/client)

  (d/create-database client {:db-name "glimmer"})


  (config/get)

  )
