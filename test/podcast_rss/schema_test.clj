(ns podcast-rss.schema-test
  (:require [clojure.test :refer [deftest is testing]]
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

(deftest predicate-schemas
  (is (schema/valid? :string "hi"))
  (is (not (schema/valid? :string 1)))
  (is (schema/valid? :non-empty-string "hi"))
  (is (not (schema/valid? :non-empty-string "")))
  (is (schema/valid? :pos-int 3))
  (is (not (schema/valid? :pos-int 0)))
  (is (not (schema/valid? :pos-int -1)))
  (is (schema/valid? :boolean false)))

(deftest map-required-and-optional
  (testing "valid minimal episode"
    (is (schema/valid? schema/Episode valid-episode)))
  (testing "missing required key is reported with a path"
    (let [errs (schema/explain schema/Episode (dissoc valid-episode :title))]
      (is (= 1 (count errs)))
      (is (= [:title] (:in (first errs))))))
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
          errs (schema/explain schema/Podcast bad)]
      (is (= [:episodes 0 :audio-url] (:in (first errs))))))
  (testing "episodes must be a sequence"
    (is (not (schema/valid? schema/Podcast (assoc valid-podcast :episodes 5))))))

(deftest validate!-behaviour
  (testing "returns the value when valid"
    (is (= valid-podcast (schema/validate! schema/Podcast valid-podcast "podcast"))))
  (testing "throws with :errors in ex-data when invalid"
    (let [ex (try (schema/validate! schema/Podcast {} "podcast")
                  (catch clojure.lang.ExceptionInfo e e))]
      (is (some? ex))
      (is (seq (:errors (ex-data ex))))
      (is (re-find #"Invalid podcast" (.getMessage ex))))))
