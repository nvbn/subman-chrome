(ns subman-chrome.content.core
  (:require-macros [swiss.arrows :refer [-<>]]))

(extend-type js/NodeList
  ISeqable
  (-seq [array] (array-seq array 0)))

(defn send-message
  "Sends message to background."
  [& params]
  (.. js/chrome -extension (sendMessage (clj->js (apply hash-map params)))))

(defn get-eztv-titles
  "Get titles for episodeds on extv."
  []
  (-<> js/document
       (.querySelectorAll "a.epinfo")
       (map #(.-innerHTML %) <>)))

(defn with-menu?
  "Should context menu be visible for this el?"
  [el]
  (.. el -classList (contains "epinfo")))

(defn on-eztv-hover
  "Bind event when episode on eztv hovered."
  [callback]
  (doseq [el (.querySelectorAll js/document "a")]
    (.addEventListener el "mouseenter"
                       #(callback (with-menu? el) (.-innerHTML el)))
    (.addEventListener el "mouseleave" #(callback false ""))))

(def host (.. js/document -location -host))

(when (= "eztv.it" host)
  (send-message :request :load-subtitles
                :titles (get-eztv-titles))
  (on-eztv-hover #(send-message :request :update-context-menu
                                :data {:with-menu? %1
                                       :title %2})))
