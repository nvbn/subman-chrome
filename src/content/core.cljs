(ns subman-chrome.content.core
  (:require-macros [cljs.core.async.macros :refer [go-loop go]]
                   [swiss.arrows :refer [-<>]])
  (:require [cljs.core.async :refer [<! >! chan]]))

(def chrome (atom nil))

(extend-type js/NodeList
  ISeqable
  (-seq [array] (array-seq array 0)))

(defn send-message
  "Sends message to background."
  [& params]
  (.. @chrome -extension (sendMessage (clj->js (apply hash-map params)))))

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
  (send-message :request :load-subtitles
                :titles (get-titles link-cls))
  (go-loop [ch (hover-chan link-cls)]
    (send-message :request :update-context-menu
                  :data (<! ch))
    (recur ch)))

(when js/chrome
  (reset! chrome js/chrome)
  (condp = (.. js/document -location -host)
    "eztv.it" (init! "epinfo")
    "thepiratebay.se" (init! "detLink")
    nil))
