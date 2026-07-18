(ns podcast-rss.schema
  "Lightweight, dependency-free schema validation for podcast data.

  The schema syntax intentionally mirrors a subset of
  [malli](https://github.com/metosin/malli): schemas are plain data, maps
  use `[:map [key schema] ...]`, entries can be marked `{:optional true}`,
  and sequences use `[:sequential schema]`. This keeps the project buildable
  in environments without Clojars access while making a later migration to
  malli close to a drop-in replacement.

  To switch to malli:
    - replace `[:non-empty-string]` with `[:string {:min 1}]`,
      `[:pos-int]` with `[:int {:min 1}]`;
    - `require '[malli.core :as m]` and use `m/validate` / `m/explain` /
      `(m/assert)` in place of the functions here;
    - the `Episode` and `Podcast` schema data below can be reused as-is."
  (:require [clojure.string :as str]))

;; ---------------------------------------------------------------------------
;; Predicate registry
;; ---------------------------------------------------------------------------

(def ^:private predicates
  {:any              {:pred (constantly true)                    :desc "anything"}
   :string           {:pred string?                             :desc "a string"}
   :non-empty-string {:pred #(and (string? %) (seq %))          :desc "a non-empty string"}
   :boolean          {:pred boolean?                            :desc "a boolean"}
   :int              {:pred int?                                :desc "an integer"}
   :pos-int          {:pred #(and (int? %) (pos? %))            :desc "a positive integer"}})

;; ---------------------------------------------------------------------------
;; Schema walking
;; ---------------------------------------------------------------------------

(declare -explain)

(defn- map-entries
  "Return the entry vectors of a `[:map ...]` schema, skipping an optional
  leading options map."
  [schema]
  (let [rst (rest schema)]
    (if (map? (first rst)) (rest rst) rst)))

(defn- parse-entry
  "Normalize a map entry to `[key opts sub-schema]`."
  [entry]
  (let [[k a b] entry]
    (if (map? a) [k a b] [k {} a])))

(defn- explain-map
  [schema value path]
  (if-not (map? value)
    [{:in path :message (str "should be a map, got " (pr-str value))}]
    (mapcat
     (fn [entry]
       (let [[k opts sub] (parse-entry entry)
             present? (contains? value k)]
         (cond
           (and (not present?) (:optional opts)) []
           (not present?) [{:in (conj path k) :message "missing required key"}]
           :else (-explain sub (get value k) (conj path k)))))
     (map-entries schema))))

(defn- explain-seq
  [schema value path]
  (if-not (sequential? value)
    [{:in path :message (str "should be a sequence, got " (pr-str value))}]
    (let [sub (second schema)]
      (apply concat
             (map-indexed (fn [i x] (-explain sub x (conj path i))) value)))))

(defn- -explain
  "Return a (possibly empty) vector of error maps `{:in path :message msg}`."
  [schema value path]
  (cond
    (keyword? schema)
    (if-let [{:keys [pred desc]} (predicates schema)]
      (if (pred value)
        []
        [{:in path :message (str "should be " desc ", got " (pr-str value))}])
      (throw (ex-info (str "Unknown schema predicate: " schema) {:schema schema})))

    (vector? schema)
    (case (first schema)
      :map (explain-map schema value path)
      :sequential (explain-seq schema value path)
      (throw (ex-info (str "Unknown schema form: " (first schema)) {:schema schema})))

    :else
    (throw (ex-info "Invalid schema" {:schema schema}))))

;; ---------------------------------------------------------------------------
;; Public API (malli-compatible names)
;; ---------------------------------------------------------------------------

(defn explain
  "Validate `value` against `schema`. Returns nil when valid, otherwise a
  seq of `{:in path :message msg}` error maps."
  [schema value]
  (seq (-explain schema value [])))

(defn valid?
  "True when `value` conforms to `schema`."
  [schema value]
  (nil? (explain schema value)))

(defn- format-error
  [{:keys [in message]}]
  (str "  " (if (seq in) (str/join " -> " (map pr-str in)) "<root>") ": " message))

(defn validate!
  "Return `value` if it conforms to `schema`, otherwise throw an ex-info
  whose message lists every problem and whose ex-data holds `:errors`."
  [schema value description]
  (if-let [errs (explain schema value)]
    (throw (ex-info (str "Invalid " description ":\n"
                         (str/join "\n" (map format-error errs)))
                    {:errors (vec errs)}))
    value))

;; ---------------------------------------------------------------------------
;; Domain schemas
;; ---------------------------------------------------------------------------

(def Episode
  [:map
   [:title :non-empty-string]
   [:description :string]
   [:audio-url :non-empty-string]
   [:audio-length   {:optional true} :pos-int]
   [:audio-type     {:optional true} :non-empty-string]
   [:guid           {:optional true} :non-empty-string]
   [:pub-date       {:optional true} :string]
   [:duration       {:optional true} :string]
   [:episode-number {:optional true} :pos-int]
   [:explicit?      {:optional true} :boolean]])

(def Podcast
  [:map
   [:title :non-empty-string]
   [:link :non-empty-string]
   [:description :string]
   [:language  {:optional true} :string]
   [:author    {:optional true} :string]
   [:email     {:optional true} :string]
   [:image     {:optional true} :string]
   [:category  {:optional true} :string]
   [:explicit? {:optional true} :boolean]
   [:self-url  {:optional true} :string]
   [:episodes [:sequential Episode]]])
