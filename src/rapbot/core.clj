(ns rapbot.core
  (:require [clojure.java.io :as io]
            [clojure.string :as s])
  (:gen-class))

(defn- ->basic-index
  []
  (with-open [r (io/reader (io/resource "cmudict-0.7b"))]
    (->> r
         (line-seq)
         (filter #(re-find #"^[A-Z]" %))
         (reduce (fn [eax line]
                   (let [[word pronunciation] (s/split line #"  ")
                         local-word (re-find #"^[^\(]+" word)]
                     (update-in eax [local-word] update :pronunciations conj pronunciation)))
                 {}))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
