(defproject podcast-rss "0.1.0"
  :description "Generate podcast RSS feeds with Clojure."
  :url "https://github.com/Pegolon/clojure-ai"
  :license {:name "MIT"}
  :dependencies [[org.clojure/clojure "1.12.5"]
                 [metosin/malli "0.20.1"]]
  :main ^:skip-aot podcast-rss.core
  :source-paths ["src"]
  :test-paths ["test"]
  :resource-paths ["resources"]
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
