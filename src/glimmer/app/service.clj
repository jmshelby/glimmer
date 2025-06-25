(ns glimmer.app.service
  (:require [datomic.client.api :as d]
            [glimmer.util :refer [uuid]]
            [glimmer.datomic.api :refer [txact-if db]]
            [glimmer.datomic.util :as du]))

(defn ping-create [! in]
  (let [$      (d/db !)
        [lat lng] (:coords in)
        tag     (:tag in)
        ping-id (uuid)
        entity  {:db/id              "ping"
                 :ping/id            ping-id
                 :ping/lat           lat
                 :ping/lng           lng
                 :ping/tag           tag
                 :entity.date/created (java.util.Date.)}
        result  (txact-if ! [entity])]
    {:out {:status :created
           :ping   (du/pull (:db-after result) 
                           [:ping/id :ping/lat :ping/lng :ping/tag :entity.date/created] 
                           (get (:tempids result) "ping"))}}))

(defn ping-query [! in]
  (let [$      (d/db !)
        tag    (:tag in)
        limit  (or (:limit in) 1000)
        query  (if tag
                 '{:query {:find [(pull ?e [:ping/id :ping/lat :ping/lng :ping/tag :entity.date/created])]
                           :in [$ ?tag]
                           :where [[?e :ping/tag ?tag]]}
                   :args [$ ?tag]
                   :limit ?limit}
                 '{:query {:find [(pull ?e [:ping/id :ping/lat :ping/lng :ping/tag :entity.date/created])]
                           :where [[?e :ping/id]]}
                   :args [$]
                   :limit ?limit})
        results (if tag
                  (d/q {:query (:query query)
                        :args [$ tag]
                        :limit limit})
                  (d/q {:query (:query query)
                        :args [$]
                        :limit limit}))]
    {:out {:pings (map first results)}}))

(defn pong-create [! in]
  (let [$        (d/db !)
        ping-ids (:pings in)
        source   (:source in)
        pongs    (for [ping-id ping-ids]
                   (let [ping-entity (d/q '{:query {:find [?e]
                                                    :in [$ ?ping-id]
                                                    :where [[?e :ping/id ?ping-id]]}
                                            :args [$ ?ping-id]})]
                     (when (seq ping-entity)
                       (cond-> {:db/id              "pong"
                                :pong/id            (uuid)
                                :pong/ping          (ffirst ping-entity)
                                :entity.date/created (java.util.Date.)}
                               source (assoc :pong/source source)))))
        valid-pongs (filter some? pongs)
        result      (txact-if ! valid-pongs)]
    {:out {:status :created
           :count  (count valid-pongs)}}))

(defn ping-pong-query [! in]
  (let [$       (d/db !)
        ping-id (:ping in)
        ping-entity (d/q '{:query {:find [?e]
                                   :in [$ ?ping-id]
                                   :where [[?e :ping/id ?ping-id]]}
                           :args [$ ?ping-id]})
        results (when (seq ping-entity)
                  (d/q '{:query {:find [(pull ?pong [:pong/id :pong/source :entity.date/created])]
                                 :in [$ ?ping-ref]
                                 :where [[?pong :pong/ping ?ping-ref]]}
                         :args [$ (ffirst ping-entity)]}))]
    {:out {:pongs (map first (or results []))}}))