(ns podcast-rss.core
  "Command-line entry point: read a podcast description from an EDN file
  and write an RSS feed to disk (or stdout)."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [podcast-rss.feed :as feed])
  (:gen-class))

(defn read-podcast
  "Read a podcast map from an EDN file path."
  [path]
  (with-open [r (io/reader path)]
    (edn/read (java.io.PushbackReader. r))))

(defn -main
  "Usage: podcast-rss <input.edn> [output.xml]

  Reads the podcast definition from <input.edn> and writes the generated
  RSS to [output.xml], or to stdout when no output path is given."
  [& args]
  (let [[input output] args]
    (when-not input
      (binding [*out* *err*]
        (println "Usage: podcast-rss <input.edn> [output.xml]"))
      (System/exit 1))
    (let [xml (feed/generate (read-podcast input))]
      (if output
        (do (spit output xml)
            (println "Wrote" output))
        (print xml)))
    (flush)))
