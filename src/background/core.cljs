(ns subman-chrome.background.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]))

; shortcuts for chrome api:
(def context-menus (.-contextMenus js/chrome))
(def extension (.-extension js/chrome))
(def tabs (.-tabs js/chrome))
(defn create-contex-menu
  [& params]
  (->> (apply hash-map params)
       clj->js
       (.create context-menus)))

(def cache (atom {}))

(def sources {0 "Addicted"
              1 "Podnapisi"
              2 "OpenSubtitles"
              3 "Subscene"
              4 "Notabenoid"
              5 "UKsubtitles"})

(defn menu-items-from-subtitles
  "Transforms subtitle to menu item."
  [{:keys [show season episode source url]}]
  {:title (str (sources source)
               ": " show " "
               (if (and season episode) (str "S" season "E" episode) ""))
   :url url})

(defn load-subtitles
  "Loads subtitles from server."
  [titles]
  (doseq [title titles]
    (go (->> (str "http://subman.io/api/search/?query=" title)
             http/get
             <!
             :body
             (take 5)
             (map menu-items-from-subtitles)
             (swap! cache assoc title)))))

(defn on-clicked
  "Open new tab with subtitle."
  [{:keys [url]}]
  (.create tabs #js {:url url}))

(defn update-context-menu
  "Update context menu when episode hovered."
  [{:keys [with-menu? title]}]
  (.removeAll context-menus)
  (when with-menu?
    (let [items (seq (@cache title))]
      (create-contex-menu :contexts [:all]
                          :id :subtitles-menu
                          :title (if items "Subtitles" "No subtitles found"))
      (doseq [item items]
        (create-contex-menu :contexts [:all]
                            :parentId :subtitles-menu
                            :title (:title item)
                            :onclick (partial on-clicked item))))))

(defn message-listener
  "Handle messages from content."
  [msg _ _]
  (let [msg (js->clj msg :keywordize-keys true)]
    (condp = (keyword (:request msg))
      :load-subtitles (load-subtitles (:titles msg))
      :update-context-menu (update-context-menu (:data msg)))))

(.. extension -onMessage (addListener message-listener))
