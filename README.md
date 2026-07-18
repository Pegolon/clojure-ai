# podcast-rss

A small [Clojure](https://clojure.org) project that generates a
[podcast RSS 2.0 feed](https://help.apple.com/itc/podcasts_connect/#/itcb54353390)
— including the Apple Podcasts (`itunes:`) and Atom extensions — from plain
Clojure/EDN data.

Built with **Clojure 1.12.5** (the latest release) and no runtime dependencies
beyond Clojure itself: the XML is emitted by a tiny built-in module, so the
output is deterministic and easy to reason about.

## Project layout

```
src/podcast_rss/xml.clj    ; dependency-free Hiccup-style XML emitter
src/podcast_rss/feed.clj   ; builds an RSS feed from a podcast map
src/podcast_rss/core.clj   ; CLI: EDN in -> RSS out
test/podcast_rss/          ; tests (incl. real XML-parser validation)
resources/sample_podcast.edn
```

## The data model

A podcast is a plain map; each episode is a map under `:episodes`:

```clojure
{:title "The Clojure Cast"
 :link "https://example.com/clojure-cast"
 :description "A weekly podcast about Clojure."
 :language "en-us"
 :author "Jane Doe"
 :email "jane@example.com"
 :image "https://example.com/artwork.jpg"
 :category "Technology"
 :explicit? false
 :self-url "https://example.com/feed.xml"
 :episodes
 [{:title "Episode 1: Why Clojure?"
   :description "We kick things off."
   :audio-url "https://example.com/ep1.mp3"
   :audio-length 24567890        ; bytes (optional)
   :audio-type "audio/mpeg"      ; optional, defaults to audio/mpeg
   :guid "clojure-cast-ep-1"     ; optional, defaults to :audio-url
   :pub-date "Tue, 03 Jun 2025 09:00:00 +0000"  ; RFC 822
   :duration "00:42:15"          ; optional
   :episode-number 1             ; optional
   :explicit? false}]}
```

Optional keys are simply omitted from the output when absent.

## Usage

### With Maven (works out of the box)

```bash
# Run the tests
mvn test

# Generate a feed from EDN (writes feed.xml)
mvn -q compile exec:java -Dexec.args="resources/sample_podcast.edn feed.xml"

# ...or print to stdout
mvn -q compile exec:java -Dexec.args="resources/sample_podcast.edn"
```

### With the Clojure CLI (`clj`)

If you have the [Clojure CLI](https://clojure.org/guides/install_clojure)
installed, a `deps.edn` is provided too:

```bash
# Generate a feed
clj -M:run resources/sample_podcast.edn feed.xml

# Run the tests
clj -X:test
```

### From the REPL / your own code

```clojure
(require '[podcast-rss.feed :as feed])

(spit "feed.xml"
      (feed/generate {:title "My Show"
                      :link "https://example.com"
                      :description "..."
                      :episodes [{:title "Ep 1"
                                  :description "..."
                                  :audio-url "https://example.com/ep1.mp3"}]}))
```

## Output

`feed/generate` returns a complete RSS document string. See
`resources/sample_podcast.edn` for the input that produces a two-episode feed
with channel-level iTunes metadata (`itunes:author`, `itunes:image`,
`itunes:category`, `itunes:owner`) and per-episode `<enclosure>`, `<pubDate>`,
`<guid>`, and duration tags. The test suite parses the generated feed with the
JDK XML parser to confirm it is well-formed.
