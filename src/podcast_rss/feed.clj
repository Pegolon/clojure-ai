(ns podcast-rss.feed
  "Build a podcast RSS 2.0 feed (with the iTunes/Apple Podcasts and Atom
  extensions) from plain Clojure data.

  A podcast is a map like:

      {:title \"My Show\"
       :link \"https://example.com\"
       :description \"A show about things.\"
       :language \"en-us\"
       :author \"Jane Doe\"
       :email \"jane@example.com\"
       :image \"https://example.com/art.jpg\"
       :category \"Technology\"
       :explicit? false
       :self-url \"https://example.com/feed.xml\"
       :episodes [ ... ]}

  and each episode is a map like:

      {:title \"Episode 1\"
       :description \"The first one.\"
       :audio-url \"https://example.com/ep1.mp3\"
       :audio-length 12345678        ; bytes, optional
       :audio-type \"audio/mpeg\"    ; optional, defaults to audio/mpeg
       :guid \"ep-1\"                ; optional, defaults to audio-url
       :pub-date \"Tue, 10 Jun 2025 09:00:00 +0000\"
       :duration \"00:42:15\"        ; optional
       :episode-number 1             ; optional
       :explicit? false}"
  (:require [podcast-rss.xml :as xml]))

(def ^:private rss-attrs
  {:version "2.0"
   :xmlns:itunes "http://www.itunes.com/dtds/podcast-1.0.dtd"
   :xmlns:atom "http://www.w3.org/2005/Atom"
   :xmlns:content "http://purl.org/rss/1.0/modules/content/"})

(defn- yes-no
  "iTunes booleans are the strings \"yes\"/\"no\"."
  [b]
  (if b "yes" "no"))

(defn- enclosure
  "Build the `<enclosure>` element with an optional length attribute."
  [{:keys [audio-url audio-length audio-type]}]
  (let [attrs (cond-> {:url audio-url
                       :type (or audio-type "audio/mpeg")}
                audio-length (assoc :length (str audio-length)))]
    [:enclosure attrs]))

(defn- item
  [{:keys [title description guid pub-date duration episode-number explicit?]
    :as episode}]
  (let [guid (or guid (:audio-url episode))]
    (into [:item
           [:title title]
           [:description description]
           [:itunes:summary description]
           (enclosure episode)]
          (remove nil?
                  [(when pub-date [:pubDate pub-date])
                   (when duration [:itunes:duration duration])
                   (when episode-number [:itunes:episode (str episode-number)])
                   [:itunes:explicit (yes-no explicit?)]
                   [:guid {:isPermaLink "false"} guid]]))))

(defn podcast->rss
  "Turn a podcast map into an RSS root element vector."
  [{:keys [title link description language author email image
           category explicit? self-url episodes]}]
  [:rss rss-attrs
   (into
    [:channel]
    (concat
     (remove nil?
             [[:title title]
              [:link link]
              [:description description]
              (when language [:language language])
              (when self-url
                [:atom:link {:href self-url
                             :rel "self"
                             :type "application/rss+xml"}])
              (when author [:itunes:author author])
              (when image [:itunes:image {:href image}])
              (when category [:itunes:category {:text category}])
              [:itunes:explicit (yes-no explicit?)]
              (when (or author email)
                [:itunes:owner
                 (when author [:itunes:name author])
                 (when email [:itunes:email email])])])
     (map item episodes)))])

(defn generate
  "Render a podcast map to a complete RSS XML document string."
  [podcast]
  (xml/emit (podcast->rss podcast)))
