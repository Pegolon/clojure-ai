(ns podcast-rss.schema
  "Type-safe schemas for podcast data, backed by
  [malli](https://github.com/metosin/malli).

  Schemas are plain data: maps use `[:map [key schema] ...]`, entries can be
  marked `{:optional true}`, and sequences use `[:sequential schema]`. The
  thin wrappers below (`valid?`, `explain`, `validate!`) delegate to
  `malli.core` / `malli.error` and give human-readable error messages."
  (:require [malli.core :as m]
            [malli.error :as me]))

;; ---------------------------------------------------------------------------
;; Domain schemas
;; ---------------------------------------------------------------------------

(def Episode
  [:map
   [:title [:string {:min 1}]]
   [:description :string]
   [:audio-url [:string {:min 1}]]
   [:audio-length   {:optional true} [:int {:min 1}]]
   [:audio-type     {:optional true} [:string {:min 1}]]
   [:guid           {:optional true} [:string {:min 1}]]
   [:pub-date       {:optional true} :string]
   [:duration       {:optional true} :string]
   [:episode-number {:optional true} [:int {:min 1}]]
   [:explicit?      {:optional true} :boolean]])

(def Podcast
  [:map
   [:title [:string {:min 1}]]
   [:link [:string {:min 1}]]
   [:description :string]
   [:language  {:optional true} :string]
   [:author    {:optional true} :string]
   [:email     {:optional true} :string]
   [:image     {:optional true} :string]
   [:category  {:optional true} :string]
   [:explicit? {:optional true} :boolean]
   [:self-url  {:optional true} :string]
   [:episodes [:sequential Episode]]])

;; ---------------------------------------------------------------------------
;; Validation API
;; ---------------------------------------------------------------------------

(defn valid?
  "True when `value` conforms to `schema`."
  [schema value]
  (m/validate schema value))

(defn explain
  "Return nil when `value` conforms to `schema`, otherwise a malli explanation
  map (see `malli.core/explain`)."
  [schema value]
  (m/explain schema value))

(defn humanize
  "Return a nested, human-readable map of errors, or nil when valid."
  [schema value]
  (some-> (m/explain schema value) me/humanize))

(defn validate!
  "Return `value` if it conforms to `schema`, otherwise throw an ex-info whose
  message describes every problem and whose ex-data holds humanized `:errors`."
  [schema value description]
  (if-let [errors (humanize schema value)]
    (throw (ex-info (str "Invalid " description ": " (pr-str errors))
                    {:errors errors}))
    value))
