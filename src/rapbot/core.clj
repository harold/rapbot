(ns rapbot.core
  (:require [clojure.java.io :as io]
            [clojure.string :as s])
  (:gen-class))

(def indexes* (atom nil))

(defn- pronunciation->last-vowel
  [pronunciation]
  (->> pronunciation
       (re-seq #"\w+\d")
       (last)
       (re-find #"[^\d]+")))

(defn- pronunciation->last-syllable
  [pronunciation]
  (re-find #"\w+\d[^\d]+$" pronunciation))

(defn- pronunciation->syllable-count
  [pronunciation]
  (count (re-seq #"\d+" pronunciation)))

(defn- ->basic-index
  []
  (with-open [r (io/reader (io/resource "cmudict-0.7b"))]
    (->> r
         (line-seq)
         (filter #(re-find #"^[A-Z]" %))
         (reduce (fn [eax line]
                   (let [[word pronunciation] (s/split line #"  ")
                         local-word (re-find #"^[^\(]+" word)]
                     (update eax local-word update :pronunciations conj pronunciation)))
                 {}))))

(defn- f->secondary-index
  [f basic-index]
  (reduce (fn [eax [word {:keys [pronunciations]}]]
            (reduce (fn [eax pronunciation]
                      (let [attr (f pronunciation)
                            current-value (get eax attr #{})]
                        (assoc eax attr (conj current-value word))))
                    eax
                    pronunciations))
          {}
          basic-index))

(defn- basic-index->last-vowel-index
  [basic-index]
  (f->secondary-index pronunciation->last-vowel basic-index))

(defn- basic-index->last-syllable-index
  [basic-index]
  (f->secondary-index pronunciation->last-syllable basic-index))

(defn- basic-index->syllable-count-index
  [basic-index]
  (f->secondary-index pronunciation->syllable-count basic-index))

(defn build-indexes!
  []
  (let [basic-index (->basic-index)]
    (reset! indexes* {:primary-index basic-index
                      :last-vowel-index (basic-index->last-vowel-index basic-index)
                      :last-syllable-index (basic-index->last-syllable-index basic-index)
                      :syllable-count-index (basic-index->syllable-count-index basic-index)}))
  :ok)

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
