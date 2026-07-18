(ns podcast-rss.schema-test
  (:require [clojure.test :refer [deftest is testing]]
            [malli.core :as m]
            [podcast-rss.schema :as schema]))

(def valid-episode
  {:title "Ep 1"
   :description "First."
   :audio-url "https://example.com/ep1.mp3"})

(def valid-podcast
  {:title "Show"
   :link "https://example.com"
   :description "Desc."
   :episodes [valid-episode]})

(deftest scalar-constraints
  (testing "non-empty string constraint"
    (is (m/validate [:string {:min 1}] "hi"))
    (is (not (m/validate [:string {:min 1}] ""))))
  (testing "positive int constraint"
    (is (m/validate [:int {:min 1}] 3))
    (is (not (m/validate [:int {:min 1}] 0)))
    (is (not (m/validate [:int {:min 1}] -1)))))

(deftest map-required-and-optional
  (testing "valid minimal episode"
    (is (schema/valid? schema/Episode valid-episode)))
  (testing "missing required key is reported at its path"
    (is (contains? (schema/humanize schema/Episode (dissoc valid-episode :title))
                   :title)))
  (testing "optional key with wrong type is rejected"
    (is (not (schema/valid? schema/Episode
                            (assoc valid-episode :audio-length "big")))))
  (testing "optional key absent is fine"
    (is (schema/valid? schema/Episode valid-episode))))

(deftest nested-sequence-validation
  (testing "valid podcast with episodes"
    (is (schema/valid? schema/Podcast valid-podcast)))
  (testing "a bad episode is reported with an indexed path"
    (let [bad (assoc-in valid-podcast [:episodes 0 :audio-url] "")
          errs (:errors (schema/explain schema/Podcast bad))]
      (is (= [:episodes 0 :audio-url] (:in (first errs))))))
  (testing "episodes must be a sequence"
    (is (not (schema/valid? schema/Podcast (assoc valid-podcast :episodes 5))))))

(deftest validate!-behaviour
  (testing "returns the value when valid"
    (is (= valid-podcast (schema/validate! schema/Podcast valid-podcast "podcast"))))
  (testing "throws with humanized :errors in ex-data when invalid"
    (let [ex (try (schema/validate! schema/Podcast {} "podcast")
                  (catch clojure.lang.ExceptionInfo e e))]
      (is (some? ex))
      (is (seq (:errors (ex-data ex))))
      (is (re-find #"Invalid podcast" (.getMessage ex))))))
