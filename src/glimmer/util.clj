(ns glimmer.util
  (:import [java.util UUID]))

(defn uuid
  "Generate a random UUID"
  []
  (UUID/randomUUID))

(defn ensure-uuid
  "Convert string to UUID, pass through if already UUID"
  [x]
  (cond
    (instance? UUID x) x
    (string? x) (UUID/fromString x)
    :else (throw (ex-info "Cannot convert to UUID" {:value x :type (type x)}))))

(defn ensure-keyword
  "Convert string to keyword, pass through if already keyword"
  [x]
  (cond
    (keyword? x) x
    (string? x) (keyword x)
    :else x))
