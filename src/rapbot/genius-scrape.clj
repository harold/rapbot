(ns rapbot.genius-scrape
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [net.cgrand.enlive-html :as html]
            [duratom.core :refer [duratom]]))

(defonce lyrics* (duratom :local-file :file-path "lyrics.edn" :init {}))


(defn- url->html
  [url]
  (let [ua "Mozilla/5.0 (Windows NT 6.2; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/32.0.1667.0 Safari/537.36"]
    (with-open [inputstream (-> (java.net.URL. url)
                                (.openConnection)
                                (doto (.setRequestProperty "User-Agent" ua))
                                (.getContent))]
      (html/html-resource inputstream))))

(defn- html->lyrics
  [html]
  (->> (html/select html [:.lyrics])
       (map html/text)
       (map s/trim)
       (interpose "\n\n")
       (apply str)))

(defn- html->title
  [html]
  (let [artist (->> (html/select html [:.song_header-primary_info-primary_artist])
                    (map html/text)
                    (map s/trim)
                    (apply str))
        title (->> (html/select html [:.song_header-primary_info-title])
                    (map html/text)
                    (map s/trim)
                    (apply str))]
    (if (and (not-empty artist) (not-empty title))
      (str artist " - " title))))

(defn- html->other-album-song-urls
  [html]
  (->> (html/select html [:.track_listing-track :a])
       (map (comp :href :attrs))))

(defn scrape-url
  [url]
  (when-not (get @lyrics* url)
    (println "Scraping:" url)
    (let [html (url->html url)
          other-album-song-urls (html->other-album-song-urls html)]
      (swap! lyrics* assoc url (html->lyrics html))
      (doseq [other-url other-album-song-urls]
        (scrape-url other-url)))))
