(ns glimmer.util
  (:import [java.util UUID]))

(defn uuid
  "Generate a random UUID"
  []
  (UUID/randomUUID))

(defn ensure-keyword
  "Convert string to keyword, pass through if already keyword"
  [x]
  (cond
    (keyword? x) x
    (string? x) (keyword x)
    :else x))
