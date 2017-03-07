(ns rapbot.core
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [clojure.set :as set]
            [clj-fuzzy.metrics :as fuzzy])
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
  (re-find #"\w+\d[^\d]*$" pronunciation))

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

(defn find-nearest-word
  [input-str]
  (if (empty? @indexes*)
    (println "Empty index....")
    (let [needle (s/upper-case input-str)]
      (->> (:primary-index @indexes*)
           (map (fn [[word _]]
                  [word (fuzzy/jaro-winkler needle word)]))
           (apply max-key second)
           (first)))))

(defn- rand-n-syllable-words
  [n]
  (loop [n n
         words []]
    (if (zero? n)
      words
      (let [syllable-count (inc (rand-int n))
            word (->> (get-in @indexes* [:syllable-count-index syllable-count])
                      (vec)
                      (rand-nth))]
        (recur (- n syllable-count) (conj words word))))))

(defn word->8-couplet
  [word]
  (let [indexes @indexes*
        base-word (find-nearest-word word)
        pronunciation (->> (get-in indexes [:primary-index base-word :pronunciations])
                           (vec)
                           (rand-nth))
        syllable-count (pronunciation->syllable-count pronunciation)
        rhymes (->> pronunciation
                    (pronunciation->last-syllable)
                    (#(get-in indexes [:last-syllable-index %]))
                    (#(set/difference % #{base-word})))
        rhyme (rand-nth (vec rhymes))
        rhyme-pronunciation (->> (get-in indexes [:primary-index rhyme :pronunciations])
                                 (vec)
                                 (rand-nth))
        rhyme-syllable-count (pronunciation->syllable-count rhyme-pronunciation)
        line1 (conj (rand-n-syllable-words (- 8 syllable-count)) base-word)
        line2 (conj (rand-n-syllable-words (- 8 rhyme-syllable-count)) rhyme)]
    [line1 line2]))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
