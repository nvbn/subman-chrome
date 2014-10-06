(ns subman-chrome.content.core
  (:require-macros [swiss.arrows :refer [-<>]]))

(extend-type js/NodeList
  ISeqable
  (-seq [array] (array-seq array 0)))

(defn send-message
  "Sends message to background."
  [& params]
  (.. js/chrome -extension (sendMessage (clj->js (apply hash-map params)))))

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

(defn on-hover
  "Bind event when episode on hovered."
  [cls callback]
  (doseq [el (.querySelectorAll js/document "a")]
    (.addEventListener el "mouseenter"
                       #(callback (with-menu? el cls) (.-innerHTML el)))
    (.addEventListener el "mouseleave" #(callback false ""))))

(def host (.. js/document -location -host))

(defn init!
  "Init extension for current page."
  [link-cls]
  (send-message :request :load-subtitles
                :titles (get-titles link-cls))
  (on-hover link-cls #(send-message :request :update-context-menu
                                    :data {:with-menu? %1
                                           :title %2})))

(js/console.log (.. js/document -location -host))

(condp = (.. js/document -location -host)
  "eztv.it" (init! "epinfo")
  "thepiratebay.se" (init! "detLink")
  nil)
