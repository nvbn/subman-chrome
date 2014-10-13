(ns subman-chrome.background.models
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [clj-di.core :refer [let-deps]])
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<! timeout]]
            [clojure.string :as string]
            [clojure.set :refer [map-invert]]
            [subman-chrome.shared.const :as const]))

(defn get-sources
  "Get sources and repeat on error."
  []
  (go-loop []
    (let [response (<! (http/get const/sources-url))]
      (if (= 200 (:status response))
        (into {} (for [[k v] (:body response)]
                   [k (string/lower-case v)]))
        (do (<! (timeout const/repeat-timeout))
            (recur))))))

(defn get-source-id
  "Get source id by name."
  [sources name]
  (if (= name const/all-sources)
    const/all-sources-id
    ((map-invert sources) name)))

(defn get-subtitles
  "Get subtitles for titles."
  [titles limit lang source]
  (go (->> (http/post const/search-url
                      {:transit-params {:queries titles
                                        :limit limit
                                        :lang lang
                                        :source source}})
           <!
           :body)))
