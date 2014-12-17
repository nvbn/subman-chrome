(ns subman-chrome.content.core
  (:require-macros [cljs.core.async.macros :refer [go-loop go]]
                   [swiss.arrows :refer [-<>]]
                   [clj-di.core :refer [let-deps]])
  (:require [cljs.core.async :refer [<! >! chan timeout]]
            [alandipert.storage-atom :refer [local-storage]]
            [clj-di.core :refer [register!]]
            [subman-chrome.shared.const :as const]
            [subman-chrome.shared.services.chrome :as c]))

(extend-type js/NodeList
  ISeqable
  (-seq [array] (array-seq array 0)))

(defn get-titles
  "Get titles for episodeds."
  [cls]
  (-<> js/document
       (.querySelectorAll (str "a." cls))
       (map #(.-innerHTML %) <>)))

(defn with-menu?
  "Should context menu be visible for this el?"
  [el cls]
  (.. el -classList (contains cls)))

(defn hover-chan
  "Return channel for hovering."
  [cls]
  (let [ch (chan)]
    (doseq [el (.querySelectorAll js/document "a")]
      (.addEventListener el "mouseenter"
                         #(go (>! ch {:with-menu? (with-menu? el cls)
                                      :title (.-innerHTML el)})))
      (.addEventListener el "mouseleave"
                         #(go (>! ch {:with-menu? false
                                      :title ""}))))
    ch))

(defn init!
  "Init extension for current page."
  [link-cls]
  (c/send-message* {:request :load-subtitles
                    :titles (get-titles link-cls)})
  (go-loop [ch (hover-chan link-cls)]
    (c/send-message* {:request :update-context-menu
                      :data (<! ch)})
    (recur ch)))

(defn update-options!
  "Update options from subman.io local storage."
  []
  (go-loop []
    (let [storage (local-storage (atom {}) :options)]       ; we can't watch to atom here
      (c/send-message* {:request :update-options
                        :options @storage})
      (<! (timeout const/options-sync-timeout))
      (recur))))

(when (c/available?)
  (register! :chrome (c/chrome-impl.))
  (condp = (.. js/document -location -host)
    "eztv.it" (init! "epinfo")
    "thepiratebay.se" (init! "detLink")
    "thepiratebay.cr" (init! "detLink")
    "kickass.so" (init! "cellMainLink")
    "subman.io" (update-options!)
    nil))
