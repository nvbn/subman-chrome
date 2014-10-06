(defproject subman-chrome "0.1.0-SNAPSHOT"
            :description "FIXME: write description"
            :url "http://example.com/FIXME"
            :license {:name "Eclipse Public License"
                      :url "http://www.eclipse.org/legal/epl-v10.html"}
            :dependencies [[org.clojure/clojure "1.6.0"]
                           [org.clojure/clojurescript "0.0-2356"]
                           [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                           [com.cognitect/transit-cljs "0.8.188"]
                           [cljs-http "0.1.16"]
                           [swiss-arrows "1.0.0"]]
            :plugins [[lein-cljsbuild "1.0.3"]]
            :cljsbuild {:builds [{:source-paths ["src/background/"]
                                  :compiler {:output-to "resources/background.js"
                                             :optimizations :whitespace
                                             :pretty-print false}}
                                 {:source-paths ["src/content/"]
                                  :compiler {:output-to "resources/content.js"
                                             :optimizations :whitespace
                                             :pretty-print false}}]})
