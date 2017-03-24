(defproject rapbot "0.1.0-SNAPSHOT"
  :description "rapping, automatically"
  :url "http://github.com/harold/rapbot"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clj-fuzzy "0.3.3"]
                 [enlive "1.1.6"]
                 [duratom "0.3.4"]]
  :main ^:skip-aot rapbot.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
