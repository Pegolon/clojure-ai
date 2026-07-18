(ns podcast-rss.xml
  "A tiny, dependency-free XML emitter.

  Elements are represented as vectors in a Hiccup-like shape:

      [:tag {:attr \"value\"} child-1 child-2 ...]

  where the attribute map is optional and children are either strings
  (rendered as escaped text) or further element vectors. Nil children
  are skipped so it is convenient to conditionally include elements."
  (:require [clojure.string :as str]))

(defn escape-text
  "Escape a string for use as XML character data."
  [s]
  (-> (str s)
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")))

(defn escape-attr
  "Escape a string for use inside a double-quoted XML attribute value."
  [s]
  (-> (escape-text s)
      (str/replace "\"" "&quot;")))

(defn- element?
  [x]
  (and (vector? x) (keyword? (first x))))

(defn- normalize
  "Return [tag attrs children] for an element vector, tolerating a
  missing attribute map."
  [[tag maybe-attrs & more]]
  (if (map? maybe-attrs)
    [tag maybe-attrs more]
    [tag {} (when (some? maybe-attrs) (cons maybe-attrs more))]))

(defn- render-attrs
  [attrs]
  (->> attrs
       (map (fn [[k v]] (str " " (name k) "=\"" (escape-attr v) "\"")))
       (str/join)))

(defn emit-element
  "Recursively render an element vector to an indented XML string.
  `depth` controls indentation (two spaces per level)."
  [node depth]
  (let [pad (apply str (repeat depth "  "))]
    (cond
      (nil? node) ""

      (string? node) (str pad (escape-text node) "\n")

      (element? node)
      (let [[tag attrs children] (normalize node)
            children (remove nil? children)
            open (str "<" (name tag) (render-attrs attrs))]
        (cond
          ;; No children -> self-closing tag.
          (empty? children)
          (str pad open "/>\n")

          ;; A single string child -> keep on one line.
          (and (= 1 (count children)) (string? (first children)))
          (str pad open ">" (escape-text (first children)) "</" (name tag) ">\n")

          ;; Otherwise nest children on their own lines.
          :else
          (str pad open ">\n"
               (apply str (map #(emit-element % (inc depth)) children))
               pad "</" (name tag) ">\n")))

      :else (throw (ex-info "Unsupported XML node" {:node node})))))

(defn emit
  "Render a root element vector to a complete XML document string,
  including the XML declaration."
  [root]
  (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
       (emit-element root 0)))
