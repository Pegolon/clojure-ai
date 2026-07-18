(ns podcast-rss.feed-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [podcast-rss.xml :as xml]
            [podcast-rss.feed :as feed]))

(def sample
  {:title "Test Show"
   :link "https://example.com"
   :description "A test podcast."
   :language "en-us"
   :author "Jane Doe"
   :email "jane@example.com"
   :image "https://example.com/art.jpg"
   :category "Technology"
   :explicit? false
   :self-url "https://example.com/feed.xml"
   :episodes
   [{:title "Ep 1"
     :description "First episode."
     :audio-url "https://example.com/ep1.mp3"
     :audio-length 12345
     :guid "ep-1"
     :pub-date "Tue, 03 Jun 2025 09:00:00 +0000"
     :duration "00:10:00"
     :episode-number 1
     :explicit? false}]})

(deftest xml-escaping
  (testing "text is escaped"
    (is (= "a &amp; b &lt;c&gt;" (xml/escape-text "a & b <c>"))))
  (testing "attributes escape quotes too"
    (is (= "say &quot;hi&quot;" (xml/escape-attr "say \"hi\""))))
  (testing "nil children are dropped and self-closing works"
    (is (= "<a/>\n" (xml/emit-element [:a nil] 0)))))

(deftest generates-well-formed-feed
  (let [out (feed/generate sample)]
    (testing "has an xml declaration and rss root"
      (is (str/starts-with? out "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"))
      (is (str/includes? out "<rss")))
    (testing "declares required namespaces"
      (is (str/includes? out "xmlns:itunes=\"http://www.itunes.com/dtds/podcast-1.0.dtd\""))
      (is (str/includes? out "xmlns:atom=\"http://www.w3.org/2005/Atom\"")))
    (testing "includes channel metadata"
      (is (str/includes? out "<title>Test Show</title>"))
      (is (str/includes? out "<language>en-us</language>"))
      (is (str/includes? out "<itunes:explicit>no</itunes:explicit>"))
      (is (str/includes? out "<atom:link")))
    (testing "renders the episode item and enclosure"
      (is (str/includes? out "<title>Ep 1</title>"))
      (is (str/includes? out "<enclosure"))
      (is (str/includes? out "url=\"https://example.com/ep1.mp3\""))
      (is (str/includes? out "length=\"12345\""))
      (is (str/includes? out "<itunes:episode>1</itunes:episode>")))))

(deftest parses-as-real-xml
  (testing "the generated feed is well-formed XML the JDK parser accepts"
    (let [out (feed/generate sample)
          factory (javax.xml.parsers.DocumentBuilderFactory/newInstance)
          _ (.setNamespaceAware factory true)
          builder (.newDocumentBuilder factory)
          doc (.parse builder
                      (java.io.ByteArrayInputStream.
                       (.getBytes out "UTF-8")))]
      (is (= "rss" (.. doc getDocumentElement getTagName))))))

(deftest optional-fields-are-omitted
  (testing "missing length/duration produce no such attributes/tags"
    (let [out (feed/generate
               (assoc sample :episodes
                      [{:title "Bare"
                        :description "No extras."
                        :audio-url "https://example.com/bare.mp3"}]))]
      (is (str/includes? out "<enclosure"))
      (is (not (str/includes? out "length=")))
      (is (not (str/includes? out "<itunes:duration>"))))))
